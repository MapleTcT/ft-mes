package com.supcon.supfusion.configuration.services.entity;

import lombok.Data;
import org.hibernate.annotations.*;
import org.hibernate.annotations.OrderBy;

import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * 消息提醒类
 *
 * @author zhushizhang
 * @version $Id$
 */
@Data
@javax.persistence.Entity
@Table(name = MessageReminder.TABLE_NAME)
public class MessageReminder implements Serializable {
    private static final long serialVersionUID = 3460095871246799712L;
    public static final String TABLE_NAME = "base_message_reminder";
    @Id
    private Long id;

    @Column(name = "TITLE", length = 1000)
    private String title;//内容标题

    @Column(name = "CONTENT", length = 4000)
    private String content;//内容
    private Integer type;// 消息类型：1为普通类型，2为紧急类型
    private String sender;//消息创建者
    @Column(name = "SENDTIME", columnDefinition = "TIMESTAMP")
    private Date sendTime;//发送时间


    @OneToMany(mappedBy = "messageReminder", cascade = {CascadeType.ALL}, targetEntity = MessageReceiver.class)
    @Fetch(FetchMode.SELECT)
    @OrderBy(clause = "code asc")
    private Set<MessageReceiver> receivers = new HashSet<MessageReceiver>();//消息接收者集合

    @Transient
    private MessageReceiver receiver;//消息接收者信息，不存入数据库

    public MessageReminder() {
        super();
    }

    public MessageReminder(String title, String content, Integer type) {
        super();
        this.title = title;
        this.content = content;
        this.type = type;
        this.sendTime = new Date();
    }

    public MessageReminder(String title, String content, String sender, Integer type) {
        super();
        this.title = title;
        this.content = content;
        this.type = type;
        this.sender = sender;
        this.sendTime = new Date();
    }
	/*@Id
//	@GenericGenerator(name = BAPGenerator.GENERATOR_NAME, strategy = BAPGenerator.STRATEGY)
//	@GeneratedValue(generator = BAPGenerator.GENERATOR_NAME)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}*/

    public void addReceiver(MessageReceiver receiver) {
        receivers.add(receiver);
    }

	
	/*当数据关联某个对象的时候需要进行oneToOne的操作
	@OneToOne
	@JoinColumn(name = "SENDER", referencedColumnName="ID")
	@Fetch(FetchMode.SELECT)
	public Staff getSender() {
		return sender;
	}
	public void setSender(Staff sender) {
		this.sender = sender;
	} 
	 */


    public Set<MessageReceiver> getReceivers() {
        return receivers;
    }

}