// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.api.handler;

import cn.hutool.core.collection.CollectionUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.hsc.api.factory.FsXmlCurlEventStrategy;
import com.hsc.common.annotation.XmlCurlEventName;
import com.hsc.common.constant.SectionNames;
import com.hsc.common.utils.StringUtils;
import com.hsc.common.xmlcurl.acl.AclConfiguration;
import com.hsc.common.xmlcurl.acl.AclList;
import com.hsc.common.xmlcurl.acl.AclNetworkLists;
import com.hsc.common.xmlcurl.acl.AclNode;
import com.hsc.common.domain.FsXmlCurl;
import com.hsc.system.domain.query.acl.FsAclQuery;
import com.hsc.system.domain.vo.acl.FsAclNodeVo;
import com.hsc.system.domain.vo.acl.FsAclVo;
import com.hsc.system.service.IFsAclService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ACL处理类
 *
 * @author danmo
 * @date 2023/09/13 22:02
 **/
@XmlCurlEventName(value = SectionNames.ACL)
@Slf4j
@Component
public class FsAclXmlCurlHandler implements FsXmlCurlEventStrategy {

    @Autowired
    private IFsAclService iFsAclService;

    @Override
    public String eventHandle(FsXmlCurl fsXmlCurl) {
        StringBuilder xml = new StringBuilder();
        try {
            xml.append(getConfiguration(fsXmlCurl.getKeyValue()));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        log.info("dialplanHandle: {}", xml);
        return xml.toString().replaceAll("networkLists", "network-lists");
    }

    private String getConfiguration(String keyValue) throws JsonProcessingException {
        AclConfiguration aclConfiguration = new AclConfiguration();
        aclConfiguration.setName(keyValue);
        aclConfiguration.setDescription("Network Lists");
        aclConfiguration.setNetworkLists(getAclNetWorkList());
        return aclConfiguration.toXmlString();
    }

    private AclNetworkLists getAclNetWorkList() {
        AclNetworkLists aclNetworkLists = new AclNetworkLists();
        aclNetworkLists.setList(getAclList());
        return aclNetworkLists;
    }

    private List<AclList> getAclList() {
        List<AclList> aclListList = new LinkedList<>();
        FsAclQuery fsAclQuery = new FsAclQuery();
        List<FsAclVo> aclServiceList = iFsAclService.getList(fsAclQuery);
        if (CollectionUtil.isNotEmpty(aclServiceList)) {
            return aclServiceList.stream().map(aclVo -> {
                AclList aclList = new AclList();
                aclList.setName(aclVo.getName());
                aclList.setAclDefault(aclVo.getDefaultType());
                List<AclNode> aclNodeList = new LinkedList<>();
                // event_socket.auto 卡的是后端连 FS 的 8021(ESL),是命门通道。
                // 最前强制注入"后端 IP 放行":FS ACL 按顺序匹配,后端 IP 命中即放行,
                // 无论前端怎么配 deny 都锁不到后端,杜绝"配错致后端连不上 FS、系统瘫"。
                if ("event_socket.auto".equals(aclVo.getName())) {
                    aclNodeList.addAll(buildBackendAllowNodes());
                }
                if (CollectionUtil.isNotEmpty(aclVo.getNodeList())) {
                    for (FsAclNodeVo fsAclNodeVo : aclVo.getNodeList()) {
                        AclNode aclNode = new AclNode();
                        aclNode.setType(fsAclNodeVo.getNodeType());
                        if (StringUtils.isNotBlank(fsAclNodeVo.getDomain())) {
                            aclNode.setDomain(fsAclNodeVo.getDomain());
                        }
                        if (StringUtils.isNotBlank(fsAclNodeVo.getCidr())) {
                            aclNode.setCidr(fsAclNodeVo.getCidr());
                        }
                        aclNodeList.add(aclNode);
                    }
                }
                aclList.setNode(aclNodeList);
                return aclList;
            }).collect(Collectors.toList());
        }
        return null;
    }

    /**
     * 构建后端防锁死放行节点:127.0.0.1 + 后端容器所有(up 且非 loopback)网卡的 IPv4。
     * 放在 event_socket.auto 的 node 列表最前,保证后端永远能连 8021。
     */
    private List<AclNode> buildBackendAllowNodes() {
        List<AclNode> nodes = new LinkedList<>();
        nodes.add(buildAllowNode("127.0.0.1/32"));
        try {
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            while (nics.hasMoreElements()) {
                NetworkInterface nic = nics.nextElement();
                if (!nic.isUp() || nic.isLoopback()) {
                    continue;
                }
                Enumeration<InetAddress> addrs = nic.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address) {
                        nodes.add(buildAllowNode(addr.getHostAddress() + "/32"));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("枚举后端网卡 IP 失败,仅放行 127.0.0.1", e);
        }
        return nodes;
    }

    private AclNode buildAllowNode(String cidr) {
        AclNode node = new AclNode();
        node.setType("allow");
        node.setCidr(cidr);
        return node;
    }


}
