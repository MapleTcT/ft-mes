package com.supcon.supfusion.configuration.services.entity;

import com.supcon.supfusion.base.entities.Staff;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * 消息接收类
 *
 * @author zhushizhang
 */
@Data
@javax.persistence.Entity
@Table(name = MessageReceiver.TABLE_NAME)
public class MessageReceiver implements Serializable {
    private static final long serialVersionUID = 2230922526154793011L;
    public static final String TABLE_NAME = "base_message_receiver";
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;//主键id
    @Column(name = "READFLAG")

    private Boolean readFlag = false;// 是否阅读

    private Staff receiver;//消息接收者
    @Column(name = "READTIME", columnDefinition = "TIMESTAMP")
    private Date readTime;//阅读时间

    @ManyToOne
    @JoinColumn(name = "MESSAGEID", referencedColumnName = "ID")
    @Fetch(FetchMode.SELECT)
    private MessageReminder messageReminder;

    public MessageReceiver() {
        super();
    }

    public MessageReceiver(Staff receiver, Date readTime, MessageReminder messageReminder) {
        super();
        this.receiver = receiver;
        this.readTime = readTime;
        this.messageReminder = messageReminder;
    }

    @OneToOne
    @JoinColumn(name = "RECEIVER", referencedColumnName = "ID")
    @Fetch(FetchMode.SELECT)
    public Staff getReceiver() {
        return receiver;
    }


    public MessageReminder getMessageReminder() {
        return messageReminder;
    }
}