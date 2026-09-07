// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.calltask.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.hsc.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;


/**
 * 客户公海表(CustomerSeas)表实体类
 *
 * @author danmo
 * @since 2025-06-27 14:13:06
 */
@Schema
@Data
@SuppressWarnings("serial")
@TableName("customer_seas")
public class CustomerSeas extends BaseEntity implements Serializable {
  private static final long serialVersionUID = 455670806553262271L;
   
    /**
     *  主键ID
     */

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;


    /**
     *  模板ID
     */
    @Schema(description = "模板ID")
    @TableField("template_id")
    private Long templateId;
     
    
    
     
    /**
     *  客户数据 
     */
    @Schema(description = "客户数据")
    @TableField("customer_info")
    private String customerInfo;
    
    
     
    /**
     * 客户联系方式(MySQL 生成列，自动取 customer_info 系统字段 phone，程序不写)
     */
    @Schema(description = "客户联系方式")
    @TableField(value = "phone", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private String phone;
    
    
     
    /**
     * 客户名称(MySQL 生成列，自动取 customer_info 系统字段 name，程序不写)
     */
    @Schema(description = "客户名称")
    @TableField(value = "name", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private String name;


    /**
     *  来源 0-手动创建 1-文件导入 2-API导入
     */
    @Schema(description = "来源 0-手动创建 1-文件导入 2-API导入")
    @TableField("source")
    private Integer source;

    /**
     * 归属坐席（sip_agent.id），NULL=公海：私海=本表两视图，不拆表（设计 D6）
     */
    @Schema(description = "归属坐席(sip_agent.id)，NULL=公海")
    @TableField("owner_id")
    private Long ownerId;

    /**
     * 最近分配时间
     */
    @Schema(description = "最近分配时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @TableField("owner_time")
    private Date ownerTime;

    /**
     * 最近被外呼时间（拨打挂断回写）
     */
    @Schema(description = "最近被外呼时间(挂断回写)")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @TableField("last_dial_time")
    private Date lastDialTime;

    /**
     * 最近外呼结果 1~7（挂断回写，NULL=未外呼）
     */
    @TableField("last_dial_result")
    private Integer lastDialResult;

    /**
     * 累计被外呼次数（拨打挂断回写原子+1）
     */
    @Schema(description = "累计被外呼次数")
    @TableField("dial_count")
    private Integer dialCount;
    
    
    
    
    
    
    


}

