// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hsc.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;


/**
 * fs管理配置表(FsConfig)表实体类
 *
 * @author danmo
 * @since 2023-10-17 11:04:58
 */
@Schema
@Data
@SuppressWarnings("serial")
@TableName("fs_config")
public class FsConfig extends BaseEntity implements Serializable {
  private static final long serialVersionUID = 364128573782783033L;
   
    /**
     *  主键ID
     */

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Integer id;


     
    /**
     *  名称 
     */
    @Schema(description = "名称")
    @TableField("name")
    private String name;
    
    
     
    /**
     *  客户端分组 
     */
    @Schema(description = "客户端分组")
    @TableField("`group`")
    private String group;
    
    
     
    /**
     *  机器地址 IP
     */
    @Schema(description = "机器地址 IP")
    @TableField("ip")
    private String ip;

  /**
   *  机器地址端口
   */
  @Schema(description = "机器地址端口")
  @TableField("port")
  private Integer port;
     
    
    
     
    /**
     *  密码 
     */
    @Schema(description = "密码")
    @TableField("password")
    private String password;
    
    
     
    /**
     *  状态 0-在线 1-下线 
     */
    @Schema(description = "状态 0-在线 1-下线(实时:后端 FsClient 内存查 ESL,不落库)")
    @TableField(exist = false)
    private Integer status;
    
    
     
    /**
     *  超时时间（秒） 
     */
    @Schema(description = "超时时间（秒）")
    @TableField("out_time")
    private Integer outTime;


}

