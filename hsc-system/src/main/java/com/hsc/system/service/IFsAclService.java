// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.service;

import com.github.pagehelper.PageInfo;
import com.hsc.common.base.IBaseService;
import com.hsc.system.domain.entity.FsAcl;
import com.hsc.system.domain.query.acl.FsAclAddQuery;
import com.hsc.system.domain.query.acl.FsAclNodeAddQuery;
import com.hsc.system.domain.query.acl.FsAclQuery;
import com.hsc.system.domain.vo.acl.FsAclVo;

import java.util.List;

/**
 * fs访问控制表(FsAcl)
 *
 * @author danmo
 * @date 2023-09-13 13:53:45
 */
public interface IFsAclService extends IBaseService<FsAcl> {

    void addList(FsAclAddQuery query);

    void addNode(FsAclNodeAddQuery query);

    void editList(FsAclAddQuery query);

    void editNode(FsAclNodeAddQuery query);

    FsAclVo getDetail(Long id);

    void delete(FsAclQuery query);

    PageInfo<FsAclVo> pageList(FsAclQuery query);

    List<FsAclVo> getList(FsAclQuery query);
}
