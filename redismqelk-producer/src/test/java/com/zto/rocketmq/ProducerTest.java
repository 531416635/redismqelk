package com.zto.rocketmq;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.alibaba.fastjson.JSON;
import com.alibaba.rocketmq.client.exception.MQBrokerException;
import com.alibaba.rocketmq.client.exception.MQClientException;
import com.alibaba.rocketmq.client.producer.DefaultMQProducer;
import com.alibaba.rocketmq.client.producer.SendResult;
import com.alibaba.rocketmq.common.message.Message;
import com.alibaba.rocketmq.remoting.exception.RemotingException;
import com.zto.entity.mqtest;
import com.zto.entity.mqtestExample;
import com.zto.service.MqtestService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(value = "classpath*:spring-mybatis1.xml")
public class ProducerTest {

	private static Logger log = LoggerFactory.getLogger(ProducerTest.class);
	@Autowired
	MqtestService mqtestService;

	@Test
	public void mqproducer() {

		/**
		 * 一个应用创建一个Producer，由应用来维护此对象，可以设置为全局对象或者单例
		 * 注意：ProducerGroundName需要由应用来保证唯一 ProducerGroup这个概念来发送普通的消息时，作用不大，
		 * 但是发送分布式消息事务时，比较关键 因为服务器会回查这个Group下的任意一个Producer
		 */

		final DefaultMQProducer producer = new DefaultMQProducer(
				"ProducerGroupName");
		// 10.10.19.14必须为本机的IP地址，并且端口号为9876
		producer.setNamesrvAddr("10.10.19.14:9876");
		producer.setInstanceName("Producer");

		/**
		 * Producer对象在使用之前必须要强调start初始化，初始化一次即可 注意：切记不可以在每次发送消息时，都调用start方法
		 */
		try {
			producer.start();
		} catch (MQClientException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		/**
		 * 下面这段代码表明一个Producer对象可以发送多个topic，多个tag消息
		 * 注意：send方法是同步调用，只要不抛异常就标识成功。但是发送成功也会有多种状态
		 * 例如消息写入MAster成功，但是save不成功，这种情况消息属于成功， 但是对于个别应用如消息可靠性要求极高
		 * 需要对这种情况做处理。另外，消息可能会存在发送失败的情况，失败重试由应用来处理
		 */
		mqtestExample example = new mqtestExample();
		example.createCriteria().andIdBetween(1, 10);
		List<mqtest> list = mqtestService.selectByExample(example);
		for (int i = 0; i < list.size(); i++) {
			try {
				{
					Message msg = new Message("helloRocket", "tag", "OrderID",
							JSON.toJSONString(list.get(i)).getBytes());
					// Message msg = new Message("helloRocket", "tag",
					// "OrderID", list.get(i).getInfo().getBytes());

					try {
						SendResult sendResult = producer.send(msg);
						log.info("消息生成成功，请继续稍候操作！");
					} catch (MQClientException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
			} catch (RemotingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (MQBrokerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			try {
				TimeUnit.MILLISECONDS.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		/**
		 * 应用退出时，要调用shutdown来清理资源，关闭网络连接，从MetaQ服务器上注销自己
		 * 注意：我们建议应用在JOBSS、Tomcat等容器的推出钩子里调用的shutdown方法
		 */
		producer.shutdown();
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

			public void run() {
				// TODO Auto-generated method stub
				producer.shutdown();
			}
		}));
		System.exit(0);
	}
}
