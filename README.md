# 牛客网项目


# 1. 搭建环境

进入网址[Spring Initializr](https://start.spring.io/)，利用这个工具来创建一个基本项目，依赖中需要加入 aop、web、thymeleaf和devtools。现在直接搜aop是搜不出来了，但是没关系，可以在项目创建好之后，自己在pom.xml中添加该依赖：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

另外注意，这里的JDK版本必须要选择对应于IDEA中的。IDEA中有好几处JDK需要配置，分别是Settings中Build下的Java Compiler还有Project Structure里project与modules部分。

下面来简单测试一下。

编写一个Controller类：

```java
package com.nowcoder.community.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/alpha")
public class AlphaController {

    @RequestMapping("/test")
    @ResponseBody
    public String test() {
        return "Hello Spring Boot";
    }
}
```

在application.properties中配置基本端口和路径

```properties
server.port=8080
server.servlet.context-path=/community
```

输入网址：localhost:8080/community/alpha/test，可以看到网页打印出Hello Spring Boot。

# 2. 发送邮件

尝试了用新浪邮箱开启代理发送邮件，但是实验时发现怎么都不行，于是上网搜索，换成了QQ邮箱就妥了。具体配置步骤可以看[这里](https://segmentfault.com/a/1190000021587834)。

在util包下创建了一个工具类 MailClient，利用Spring框架提供的 JavaMailSender 类来完成发送邮件的功能。

```java
package com.nowcoder.community.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

@Component
public class MailClient {

    //记录打印日志
    private static final Logger logger = LoggerFactory.getLogger(MailClient.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    public void sendMail(String to, String subject, String content) {
        try {
            //创建信息
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message);

            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);

            mailSender.send(helper.getMimeMessage());
        } catch (MessagingException e) {
            logger.error("邮件发送失败:" + e.getMessage());
        }
    }
}
```

在实际调用的时候，可以使用发送文本邮件，还是html网页邮件，具体示例如下：

```java
@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class MailTest {

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Test
    public void testTextMail() {
        mailClient.sendMail("woojou@sina.com", "Test", "Welcome");
    }

    @Test
    public void testHtmlMail() {
        Context context = new Context();
        context.setVariable("username", "Cindy");
        //发送一个html网页
        String content = templateEngine.process("/mail/demo", context);
        //System.out.println(content);

        mailClient.sendMail("woojou@sina.com", "HtmlTest", content);

    }
}
```

# 3. Redis

- Redis是一款基于键值对的NoSQL数据库，它的值支持多种数据结构： 字符串(strings)、哈希(hashes)、列表(lists)、集合(sets)、有序集合(sorted sets)等。 
- Redis将所有的数据都存放在内存中，所以它的读写性能十分惊人。 同时，Redis还可以将内存中的数据以快照（一次性把内存中所有数据存入到硬盘上，可能会造成阻塞，不适合实时去做）或日志（每执行一条Redis命令就执行一次，实时性好，但恢复慢）的形式保存到硬盘上，以保证数据的安全性。 
- Redis典型的应用场景包括：缓存、排行榜、计数器、社交网络、消息队列等

记得在环境变量的path里配好redis的下载路径

## 3.1 常用命令

redis-cli： 使用redis

select 0-15： redis中自动配置了0-15共16个库

**String 类型：**

- set  <key>  <value>：写入

- get <key>：读取

- incr <key>：加一

- desr <key>：减一

**HashMap 类型：**

- hset <key> <<key> <value>>：存入
- hget <key> <key>：读取
- hgetall <key>：读取所有

**LinkedList 类型：**

- lpush <key> <value....> ：左进右出地存取
- lindex <key> <id>：取出
- lrange <key> <id1> <id2>：id1到id2范围内的所有数
- rpop <key>：从右侧弹出一个值

**HashSet 类型（无序，不能重复）：**

- sadd <key> <member...>：存入（多个值）
- scard <key>：统计有多少元素
- spop <key>：随机弹出一个元素
- smembers key：查看集合中的所有元素

**SortedSet 类型：**

- zadd <key> <score> <member>：存入，根据score来排序存储
- zcard <key>：统计有多少元素
- zscore <key> <member>：查看分数
- zrank <key> <member>：查看排名
- zrange <key> <start> <stop>：取出排名范围内所有的成员

**全局可用**

- keys *：查看所有的key
- key <str>*：查看以str开头的所有类型
- type <key>：查看类型
- exists <key>：查看是否存在，1表示存在，0不存在
- del <key>：删除
- expire <key> <seconds>：指定过多久之后key就被删除

## 3.2 Spring整合Redis

pom.xml配置：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

application.properties配置：

```properties
spring.redis.database=11 #这里你写0-15随便一个数都可以
spring.redis.host=localhost
spring.redis.port=6379
```

SpringBoot自动帮我们在容器中生成了一个RedisTemplate和一个StringRedisTemplate。但是，这个RedisTemplate的泛型是<Object,Object>，我们不咋用。我们需要一个泛型为<String,Object>形式的RedisTemplate。并且，这个原始的RedisTemplate没有设置数据存在Redis时，key及value的序列化方式。

```java
package com.nowcoder.community.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        template.setKeySerializer(RedisSerializer.string());
        template.setValueSerializer(RedisSerializer.string());
        template.setHashKeySerializer(RedisSerializer.string());
        template.setHashValueSerializer(RedisSerializer.string());
        template.afterPropertiesSet();

        return template;
    }
}
```

## 3.3 Redis使用场景

[Redis的8大应用场景 - 云+社区 - 腾讯云 (tencent.com)](https://cloud.tencent.com/developer/article/1415674)

## 3.4 Redis高级数据应用

- UV（Unique Visitor） 
  - 独立访客，需通过用户 IP 排重统计数据。 
  - 每次访问都要进行统计。 
  - HyperLogLog ，性能好，且存储空间小。 
- DAU（Daily Active User） 
  - 日活跃用户，需通过用户 ID 排重统计数据。 
  - 访问过一次，则认为其活跃。 
  - Bitmap，性能好、且可以统计精确的结果。

# 4. Kafka

- Kafka是一个分布式的流媒体平台。 
- 应用：消息系统、日志收集、用户行为追踪、流式处理。
- Kafka特点 - 高吞吐量、消息持久化、高可靠性、高扩展性。
- Kafka术语 
  - Broker（服务器）、Zookeeper （用于管理其它集群，并不从属于kafka）
  - Topic、Partition（分区）、Offset 
  - Leader Replica（主副本，可以处理请求） 、Follower Replica（随从副本，不可以处理请求）

## 4.1 安装

下载后直接解压，我放在D盘

然后修改/config/zookeeper.properties:

```properties
# 第16行
dataDir=d:/data/zookeeper
```

/config/server.properties：

```properties
# 第60行
log.dirs=d:/data/kafka-logs
```

这里修改一下个性化设置/config/consumer.properties：（不改也行哈）

```properties
group.id=test-consumer-group
```

## 4.2 使用

看[这里](https://blog.csdn.net/weixin_43085439/article/details/106403168)

开两个命令行窗口，分别运行

```
bin\windows\zookeeper-server-start.bat config\zookeeper.properties
```

```
bin\windows\kafka-server-start.bat config\server.properties
```

## 4.3 Spring整合Kafka

引入依赖

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

配置application.properties

```properties
#KafkaProperties
spring.kafka.bootstrap-servers=localhost:9092
#这里要和/config/consumer.properties文件里的相同
spring.kafka.consumer.group-id=community-consumer-group
spring.kafka.consumer.enable-auto-commit=true
#每3000ms自动提交一次
spring.kafka.consumer.auto-commit-interval=3000
#打开端口监听
listeners=PLAINTEXT://localhost:9092
```

测试一下，记得把Kafka 启动：

```java
package com.nowcoder.community;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class KafkaTest {

    @Autowired
    private KafkaProducer kafkaProducer;


    @Test
    public void testKafka() {
        kafkaProducer.sendMessage("test", "哈哈哈");
        kafkaProducer.sendMessage("test", "耶耶耶");

        try {
            Thread.sleep(1000*10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

//生产者需要我们主动调用
@Component
class KafkaProducer {
    @Autowired
    private KafkaTemplate kafkaTemplate;

    public void sendMessage(String topic, String content) {
        kafkaTemplate.send(topic, content);
    }
}

//消费者是自动的，当队列中有消息就会自动处理
@Component
class KafkaConsumer {
    @KafkaListener(topics = {"test"})
    public void handleMessage(ConsumerRecord record) {
        System.out.println(record.value());
    }
}
```

# 5. Elasticsearch

- Elasticsearch简介 

  - 一个分布式的、Restful风格的搜索引擎。 
  - 支持对各种类型的数据的检索。 
  - 搜索速度快，可以提供实时的搜索服务。 
  - 便于水平扩展，每秒可以处理PB级海量数据。 

- Elasticsearch术语 

  - 索引(database)、~~类型(table)~~、文档(row)、字段(col)。

    6.0以后废弃了类型，索引直接表示table

  - 集群(多台服务器在一起)、节点(每一台服务器)、分片(对索引的进一步划分)、副本(对分片的备份)。

## 5.1 安装

[免费且开放的搜索：Elasticsearch、ELK 和 Kibana 的开发者 | Elastic](https://www.elastic.co/cn/) 下载好直接解压

**配置文件**：elasticsearch.yml

```xml
# ======================== Elasticsearch Configuration =========================
#
# NOTE: Elasticsearch comes with reasonable defaults for most settings.
#       Before you set out to tweak and tune the configuration, make sure you
#       understand what are you trying to accomplish and the consequences.
#
# The primary way of configuring a node is via this file. This template lists
# the most important settings you may want to configure for a production cluster.
#
# Please consult the documentation for further information on configuration options:
# https://www.elastic.co/guide/en/elasticsearch/reference/index.html
#
# ---------------------------------- Cluster -----------------------------------
#
# Use a descriptive name for your cluster:
#
cluster.name: nowcoder
#
# ------------------------------------ Node ------------------------------------
#
# Use a descriptive name for the node:
#
#node.name: node-1
#
# Add custom attributes to the node:
#
#node.attr.rack: r1
#
# ----------------------------------- Paths ------------------------------------
#
# Path to directory where to store the data (separate multiple locations by comma):
#
path.data: d:/data/elasticsearch-6.4.3/data
#
# Path to log files:
#
path.logs: d:/data/elasticsearch-6.4.3//logs
#
# ----------------------------------- Memory -----------------------------------
#
# Lock the memory on startup:
#
#bootstrap.memory_lock: true
#
# Make sure that the heap size is set to about half the memory available
# on the system and that the owner of the process is allowed to use this
# limit.
#
# Elasticsearch performs poorly when the system is swapping the memory.
#
# ---------------------------------- Network -----------------------------------
#
# Set the bind address to a specific IP (IPv4 or IPv6):
#
#network.host: 192.168.0.1
#
# Set a custom port for HTTP:
#
#http.port: 9200
#
# For more information, consult the network module documentation.
#
# --------------------------------- Discovery ----------------------------------
#
# Pass an initial list of hosts to perform discovery when new node is started:
# The default list of hosts is ["127.0.0.1", "[::1]"]
#
#discovery.zen.ping.unicast.hosts: ["host1", "host2"]
#
# Prevent the "split brain" by configuring the majority of nodes (total number of master-eligible nodes / 2 + 1):
#
#discovery.zen.minimum_master_nodes: 
#
# For more information, consult the zen discovery module documentation.
#
# ---------------------------------- Gateway -----------------------------------
#
# Block initial recovery after a full cluster restart until N nodes are started:
#
#gateway.recover_after_nodes: 3
#
# For more information, consult the gateway module documentation.
#
# ---------------------------------- Various -----------------------------------
#
# Require explicit names when deleting indices:
#
#action.destructive_requires_name: true
```

**环境变量**：拷贝好bin目录的路径，存放到环境变量的path下

**中文分词插件**：[Release v6.4.3 · medcl/elasticsearch-analysis-ik · GitHub](https://github.com/medcl/elasticsearch-analysis-ik/releases/tag/v6.4.3)下载与ES对应的版本，解压缩到 ...\elasticsearch-6.4.3\plugins\ik

​		\config\IKAnalyzer.cfg.xml中规定了自己的扩展字典，你可以在相应的文件中自己配置

**下载Postman**：[Postman API Platform | Sign Up for Free](https://www.postman.com/)



ES在启动时，默认申请1g内存，在学习阶段，这太耗内存了，可以调小一点。大家可以修改其config目录下的jvm.options文件，将初识内存调整为256m，将最大内存调整为512m

## 5.2 使用

启动ES：直接双击 D:\elasticsearch-6.4.3\bin\elasticsearch.bat

查看健康状况：curl -X GET "localhost:9200/_cat/health?v"

查看节点：curl -X GET "localhost:9200/_cat/nodes?v"

查看索引：curl -X GET "localhost:9200/_cat/indices?v"

建立索引：curl -X PUT "localhost:9200/{要建立的索引}"

删除索引：curl -X DELETE "localhost:9200/{要删除的索引}"

利用postman：

![](https://img-blog.csdnimg.cn/8e5301723f6240f387124d6e581d072b.png)

![](https://img-blog.csdnimg.cn/5c2e3aac7f0e4fffb884274ec1799cfe.png)

![](https://img-blog.csdnimg.cn/e8ae00f2add9423495bb9d5fbc20e5de.png)

使用中文存数据：

```
localhost:9200/test/_doc/1
{
    "title": "互联网招聘",
    "content": "招聘程序员"
}

localhost:9200/test/_doc/2
{
    "title": "互联网求职",
    "content": "寻求运营岗位"
}

localhost:9200/test/_doc/3
{
    "title": "实习生推荐",
    "content": "本人在一家互联网公司任职，可推荐实习开发岗位"
}
```

查询搜索：

![](https://img-blog.csdnimg.cn/9a69b3d739cc4b2eb7672e8655f690ac.png)

![](https://img-blog.csdnimg.cn/d07f99d345394ec8aca3bc8b44781db0.png)

复杂的查询：

![](https://img-blog.csdnimg.cn/3505771e587440158a851202e989598e.png)

## 5.3 Spring整合Elasticsearch

- 引入依赖 
  - spring-boot-starter-data-elasticsearch 

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
</dependency>
```

- 配置Elasticsearch 
  - cluster-name、cluster-nodes 

```properties
#ElasticsearchProperties
spring.data.elasticsearch.cluster-name=nowcoder
spring.data.elasticsearch.cluster-nodes=127.0.0.1:9300
```

- 修改Netty配置
  - Redis和Elasticsearch的netty会引起冲突，在CommunityApplication类中配置

```java
@PostConstruct
public void init() {
    //解决Netty启动冲突问题
    //see Netty4Utils.setAvailableProcessors()
    System.setProperty("es.set.netty.runtime.available.processors", "false");
}
```

- 注解配置实体类

```java
package com.nowcoder.community.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;

//type逐渐被废弃了这里只是一个简单的占位 shards分片 replicas副本
@Document(indexName = "discusspost", type = "_doc", shards = 6, replicas = 3)
public class DiscussPost {

    @Id
    private int id;

    @Field(type = FieldType.Integer)
    private int userId;

    // analyzer是存储时拆词的标准依据 searchAnalyzer是搜索时的
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String content;

    @Field(type = FieldType.Integer)
    private int type;

    @Field(type = FieldType.Integer)
    private int status;

    @Field(type = FieldType.Date)
    private Date createTime;

    @Field(type = FieldType.Integer)
    private int commentCount;

    @Field(type = FieldType.Double)
    private double score;

    ...
}
```

- Spring Data Elasticsearch 
  - ElasticsearchTemplate 
  - ElasticsearchRepository

```java
package com.nowcoder.community;


import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.dao.elasticsearch.DiscussPostRepository;
import com.nowcoder.community.entity.DiscussPost;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class ElasticsearchTests {
    @Resource
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private DiscussPostRepository discussPostRepository;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Test
    public void testInsert() {
        discussPostRepository.save(discussPostMapper.selectDiscussPostById(241));
        discussPostRepository.save(discussPostMapper.selectDiscussPostById(242));
        discussPostRepository.save(discussPostMapper.selectDiscussPostById(243));
    }

    @Test
    public void testInsertList() {
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(101, 0, 100));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(102, 0, 100));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(103, 0, 100));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(111, 0, 100));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(112, 0, 100));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(131, 0, 100));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(132, 0, 100));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(133, 0, 100));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(134, 0, 100));
    }

    @Test
    public void testUpdate() {
        DiscussPost post =discussPostMapper.selectDiscussPostById(231);
        post.setContent("我是新人，使劲儿灌水");
        discussPostRepository.save(post);
    }

    @Test
    public void testDelete() {
        //discussPostRepository.deleteById(231);
        discussPostRepository.deleteAll();
    }

    @Test
    public void testSearchByRepository() {
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content"))
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .withPageable(PageRequest.of(0, 10))
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("<em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("<em>")
                ).build();

        Page<DiscussPost> page = discussPostRepository.search(searchQuery);
        System.out.println(page.getTotalElements());
        System.out.println(page.getTotalPages());
        System.out.println(page.getNumber());
        System.out.println(page.getSize());

        for(DiscussPost post: page) {
            System.out.println(post);
        }

    }

    @Test
    public void testSearchByTemplate() {
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content"))
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .withPageable(PageRequest.of(0, 10))
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("<em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("<em>")
                ).build();


        Page<DiscussPost> page = elasticsearchTemplate.queryForPage(searchQuery, DiscussPost.class, new SearchResultMapper() {
            @Override
            public <T> AggregatedPage<T> mapResults(SearchResponse searchResponse, Class<T> aClass, Pageable pageable) {
                SearchHits hits = searchResponse.getHits();
                if(hits.getTotalHits() <= 0) {
                    return null;
                }

                List<DiscussPost> list = new ArrayList<>();
                for(SearchHit hit : hits) {
                    DiscussPost post = new DiscussPost();

                    String id = hit.getSourceAsMap().get("id").toString();
                    post.setId(Integer.valueOf(id));

                    String userId = hit.getSourceAsMap().get("userId").toString();
                    post.setUserId(Integer.valueOf(userId));

                    String title = hit.getSourceAsMap().get("title").toString();
                    post.setTitle(title);

                    String content = hit.getSourceAsMap().get("content").toString();
                    post.setContent(content);

                    String status = hit.getSourceAsMap().get("status").toString();
                    post.setStatus(Integer.valueOf(status));

                    String createTime = hit.getSourceAsMap().get("createTime").toString();
                    post.setCreateTime(new Date(Long.valueOf(createTime)));

                    String commentCount = hit.getSourceAsMap().get("commentCount").toString();
                    post.setCommentCount(Integer.valueOf(commentCount));

                    // 处理高亮显示的结果
                    HighlightField titleField = hit.getHighlightFields().get("title");
                    if (titleField != null) {
                        post.setTitle(titleField.getFragments()[0].toString());
                    }

                    HighlightField contentField = hit.getHighlightFields().get("content");
                    if (contentField != null) {
                        post.setContent(contentField.getFragments()[0].toString());
                    }

                    list.add(post);
                }

                return new AggregatedPageImpl(list, pageable,
                        hits.getTotalHits(), searchResponse.getAggregations(), searchResponse.getScrollId(), hits.getMaxScore());
            }
        });

        System.out.println(page.getTotalElements());
        System.out.println(page.getTotalPages());
        System.out.println(page.getNumber());
        System.out.println(page.getSize());

        for(DiscussPost post: page) {
            System.out.println(post);
        }
    }
}
```

# 6. Spring Security

spring security底层就是filter，也就是说在Interceptor和controller之前执行。spring security包含了认证和授权。认证后将一个token存入，后续的授权都依赖于这个先前存入的结果。不过在我们这个项目里，是绕过了spring security走自己的login网页逻辑，在登录凭证拦截器中人为把token结果存入，便于后序的过滤功能。

使用时，在spring boot中直接引入依赖即可

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

通过一个spring security的配置类来进行我们自定义的权限控制的操作。

```java
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter implements CommunityConstant {

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/resources/**");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // 授权
        http.authorizeRequests()
                .antMatchers(
                        "/user/setting",
                        "/user/upload",
                        "/like",
                        "/discuss/add",
                        "comment/add/**",
                        "/letter/**",
                        "/notice/**",
                        "follow",
                        "unfollow"
                )
                .hasAnyAuthority(
            // 任意用户都可以执行的操作，前提是必须登录
                        AUTHORITY_ADMIN,
                        AUTHORITY_MODERATOR,
                        AUTHORITY_USER
                )
                .antMatchers(
                        "/discuss/top",
                        "/discuss/wonderful"
                )
                .hasAnyAuthority(
            // 版主才可以进行的操作，置顶+加精
                        AUTHORITY_MODERATOR
                )
                .antMatchers(
                        "/discuss/delete",
                        "/data/**"
                )
                .hasAnyAuthority(
            // 管理员可以执行的操作
                        AUTHORITY_ADMIN
                )
                .anyRequest().permitAll()
                .and().csrf().disable();

        // 权限不够时的处理
        http.exceptionHandling()
                .authenticationEntryPoint(new AuthenticationEntryPoint() {
                    //没有登陆的情况
                    @Override
                    public void commence(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AuthenticationException e) throws IOException, ServletException {
                        String header = httpServletRequest.getHeader("x-requested-with");
                        if ("XMLHttpRequest".equals(header)) {
                            // 异步请求
                            httpServletResponse.setContentType("application/plain;charset=utf-8");
                            PrintWriter writer = httpServletResponse.getWriter();
                            writer.write(CommunityUtil.getJSONString(403, "您还没有登录！"));
                        } else {
                            httpServletResponse.sendRedirect(httpServletRequest.getContextPath() + "/login");
                        }
                    }
                })
                .accessDeniedHandler(new AccessDeniedHandler() {
                    // 权限不足
                    @Override
                    public void handle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AccessDeniedException e) throws IOException, ServletException {
                        String header = httpServletRequest.getHeader("x-requested-with");
                        if ("XMLHttpRequest".equals(header)) {
                            // 异步请求
                            httpServletResponse.setContentType("application/plain;charset=utf-8");
                            PrintWriter writer = httpServletResponse.getWriter();
                            writer.write(CommunityUtil.getJSONString(403, "您没有访问权限！"));
                        } else {
                            httpServletResponse.sendRedirect(httpServletRequest.getContextPath() + "/denied");
                        }
                    }
                });

        // Security底层默认会拦截/logout请求,进行退出处理.
        // 覆盖它默认的逻辑,才能执行我们自己的退出代码.
        // 随便给一个路径
        http.logout().logoutUrl("/securitylogout");
    }
}
```



# 7. 注册功能

## 7.1 dao

提供一个插入用户信息的方法

```java
int insertUser(User user);
```

```xml
<sql id="insertFields">
    username, password, salt, email, type, status, activation_code, header_url, create_time
</sql>

<insert id="insertUser" parameterType="User" keyProperty="id">
    insert into user (<include refid="insertFields"/>)
    values (#{username}, #{password}, #{salt}, #{email}, #{type}, #{status}, #{activationCode}, #{headerUrl}, #{createTime})
</insert>
```

## 7.2 service

1. 先对所有字段进行判空
2. 验证账号与邮箱是否已经被注册过
3. 创建一个User对象，将其插入进数据库表中
4. 生成激活码，用于生成一个激活链接，然后给用户发送激活邮件（利用了Thymeleaf模板发送HTML网页邮件）
5. 点击激活链接后，判断激活码是否正确以及用户是否已经激活。如果没有被激活过，现在修改其状态码为1

## 7.3 controller

```java
@RequestMapping(path = "/register", method = RequestMethod.POST)
public String register(Model model, User user) {
    Map<String, Object> map = userService.register(user);
    if (map == null || map.isEmpty()) {
        model.addAttribute("msg", "注册成功，我们已经向您的邮箱发送了一封激活邮件，请完成激活操作");
        model.addAttribute("target", "/index");
        return "/site/operate-result";
    } else {
        //返回错误信息
        model.addAttribute("usernameMsg", map.get("usernameMsg"));
        model.addAttribute("emailMsg", map.get("emailMsg"));
        model.addAttribute("passwordMsg", map.get("passwordMsg"));
        return "/site/register";
    }
}
```

点击了激活链接后的操作，主要是一些页面提示状态的显示。

```java
@RequestMapping(path = "/activation/{userId}/{code}", method = RequestMethod.GET)
public String activation(Model model, @PathVariable("userId") int userId, @PathVariable("code") String code) {
    int result = userService.activation(userId, code);
    if (result == ACTIVATION_SUCCESS) {
        model.addAttribute("msg", "激活成功");
        model.addAttribute("target", "/login");
    } else if (result == ACTIVATION_REPEAT) {
        model.addAttribute("msg", "您的账号已被激活，请勿重复激活");
        model.addAttribute("target", "/index");
    } else {
        model.addAttribute("msg", "激活失败");
        model.addAttribute("target", "/index");
    }
    return "/site/operate-result";
}
```

# 8. 登录功能

## 8.1 验证码生成

### 8.1.1 配置类

spring没有提供kaptcha的自动注入，需要我们自己写配置类

```java
@Configuration
public class KaptchaConfig {

    //spring没有提供自动注入需要我们自己写配置类
    @Bean
    public Producer kaptchaProducer() {
        Properties properties = new Properties();
        
        // 设置尺寸
        properties.setProperty("kaptcha.image.width", "100");
        properties.setProperty("kaptcha.image.height", "40");
        // 设置字体
        properties.setProperty("kaptcha.textproducer.font.size", "32");
        properties.setProperty("kaptcha.textproducer.font.color", "0,0,0"); //黑色
        // 设置验证码的字母范围
        properties.setProperty("kaptcha.textproducer.char.string", "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"); //字母范围
        // 验证码长度为4
        properties.setProperty("kaptcha.textproducer.char.length", "4");
        // 没有噪点
        properties.setProperty("kaptcha.noise.impl", "com.google.code.kaptcha.impl.NoNoise");

        DefaultKaptcha kaptcha = new DefaultKaptcha();
        Config config = new Config(properties);
        kaptcha.setConfig(config);
        return kaptcha;
    }
}
```

### 8.1.2 获取验证码

```java
@RequestMapping(path = "/kaptcha", method = RequestMethod.GET)
public void getKaptcha(HttpServletResponse response/*, HttpSession session*/) {
    //生成验证码
    String text = kaptchaProducer.createText();
    BufferedImage image = kaptchaProducer.createImage(text);

    // 生成随机的凭证
    String kaptchaOwner = CommunityUtil.generateUUID();
    String kaptchaKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
    
    // 存入Cookie
    Cookie cookie = new Cookie("kaptchaOwner", kaptchaOwner);
    cookie.setMaxAge(60);
    cookie.setPath(contextPath);
    response.addCookie(cookie);
    // 存入Redis,键（随机凭证）值（验证码）
    redisTemplate.opsForValue().set(kaptchaKey, text, 60, TimeUnit.SECONDS);

    // 将图片输出给浏览器
    response.setContentType("image/png");
    try {
        OutputStream os = response.getOutputStream();
        ImageIO.write(image, "png", os);
    } catch (IOException e) {
        logger.error("验证码响应失败: " + e.getMessage());
    }
}
```

## 8.2 service

1. 字段判空，账号密码验证，以及是否已经激活判断
2. 生成登录凭证，并存入Redis
3. 如果登录失败，就存入一些错误信息

## 8.3 controller

1. 从Cookie中取到验证码的凭证，去redis中搜寻到验证码，进行比对
2. 调用业务层的登录函数，获得到Loginticket的ticket
3. 将ticket存入Cookie，设置cookie生效范围与时间。加入到Response中。

## 8.4 登录凭证拦截器

### 8.4.1 preHandle

拦截器的preHandle方法**在Controller处理之前进行调用**，也就是在请求方法之前调用。

在这一步，根据之前在Cookie中存入的ticket去Redis中寻找登录凭证，然后根据登录凭证去查找登录的用户信息。将该用户对象存入到hostholder中。构建用户认证的结果，并存入SecurityContext，以便于Security进行授权。

```java
@Override
public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    //从cookie中获取凭证
    String ticket = CookieUtil.getValue(request, "ticket");
    if (ticket != null) {
        LoginTicket loginTicket = userService.findByTicket(ticket);
        // 登录凭证不为空，且登录凭证没有过期
        if (loginTicket != null && loginTicket.getStatus() == 0 && loginTicket.getExpired().after(new Date())) {
            User user = userService.findUserById(loginTicket.getUserId());
            hostHolder.setUser(user);

            // 构建用户认证的结果token,并存入SecurityContext,以便于Security进行授权.
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                user, user.getPassword(), userService.getAuthorities(user.getId()));
            SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
        }
    }
    return true;
}
```

这里调用的业务层，`findUserById`其实运用到了Redis，而不是直接的Mysql数据库。因为每次Controller请求是非常频繁的，每次都要做一次数据库查询会大大降低效率。

```java
public User findUserById(int id) {
    User user = getCache(id);
    if(user == null) {
        user = initCache(id);
    }
    return user;
}
// 1.优先从缓存中取值
private User getCache(int userId) {
    String redisKey = RedisKeyUtil.getUserKey(userId);
    return (User) redisTemplate.opsForValue().get(redisKey);
}

// 2.取不到时初始化缓存数据
private User initCache(int userId) {
    User user = userMapper.selectById(userId);
    String redisKey = RedisKeyUtil.getUserKey(userId);
    redisTemplate.opsForValue().set(redisKey, user, 3600, TimeUnit.SECONDS);
    return user;
}

// 3.数据变更时清除缓存数据
private void clearCache(int userId) {
    String redisKey = RedisKeyUtil.getUserKey(userId);
    redisTemplate.delete(redisKey);
}
```

这个HostHolder，是多线程操作，每次能从线程池中返回当前线程的用户。`ThreadLocal` 叫做本地线程变量，意思是说，`ThreadLocal` 中填充的的是当前线程的变量，该变量对其他线程而言是封闭且隔离的，`ThreadLocal` 为变量在每个线程中创建了一个副本，这样每个线程都可以访问自己内部的副本变量。

### 8.4.2 postHandle

postHandle是进行处理器拦截用的，它的执行时间是在处理器进行处理之后，也就是在**Controller的方法调用之后执行**，但是它会在DispatcherServlet进行**视图的渲染之前执行**，也就是说在这个方法中你可以**对ModelAndView进行操作**。

在这里，把之前加入到hostholder中的用户对象加入到ModelAndView中。这里是为了页面显示所需的user信息。

```java
@Override
public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    User user = hostHolder.getUser();
    if(user != null && modelAndView != null) {
        modelAndView.addObject("loginUser", user);
    }
}
```

### 8.4.3 afterCompletion

afterCompletion方法将在整个请求完成之后，也就是视图渲染之后执行，主要用于清理资源。

这里，只需要清除hostholder。

```java
@Override
public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
    hostHolder.clear();
}
```

# 9. 头像上传

## 9.1 本地上传

### 9.1.1 前端

必须是POST请求，enctype=“multipart/form-data”

```html
<form class="mt-5" method="post" enctype="multipart/form-data" th:action="@{/user/upload}">
    <div class="form-group row mt-4">
        <label class="col-sm-2 col-form-label text-right">选择头像:</label>
        <div class="col-sm-10">
            <div class="custom-file">
                <input type="file" class="custom-file-input" th:class="|custom-file-input ${error!=null?'is-invalid':''}|"
                       id="head-image" name="headerImage"  lang="es" required="">
                <label class="custom-file-label" for="head-image" data-browse="文件">选择一张图片</label>
                <div class="invalid-feedback" th:text="${error}">
                    错误啦！
                </div>
            </div>
        </div>
    </div>
    <div class="form-group row mt-4">
        <div class="col-sm-2"></div>
        <div class="col-sm-10 text-center">
            <button type="submit" class="btn btn-info text-white form-control">立即上传</button>
        </div>
    </div>
</form>
```

### 9.1.2 dao

```java
int updateHeader(int id, String headerUrl);
```

```xml
<update id="updateHeader">
    update user set header_url = #{headerUrl} where id = #{id}
</update>
```

### 9.1.3 service

```java
public int updateHeader(int userId, String headerUrl) {
    int rows = userMapper.updateHeader(userId, headerUrl);
    // 做更改后一定要清除缓存
    clearCache(userId);
    return rows;
}
```

### 9.1.4 controller

利用SpringMVC的MultipartFile来上传文件

1. 首先要进行一些常规的判空、判错的操作。

2. 然后为该文件创建一个随机的文件路径，创建文件类，文件导入到该地址

```java
@RequestMapping(path = "/upload", method = RequestMethod.POST)
public String uploadHeader(MultipartFile headerImage, Model model) {
    if (headerImage == null) {
        model.addAttribute("error", "您还没有选择图片！");
        return "/site/setting";
    }

    String fileName = headerImage.getOriginalFilename();
    String suffix = fileName.substring(fileName.lastIndexOf("."));

    if (StringUtils.isBlank(suffix)) {
        model.addAttribute("error", "图片格式不正确！");
        return "/site/setting";
    }

    fileName = CommunityUtil.generateUUID() + suffix;
    // uploadPath:本地上传路径
    String dest = uploadPath + "/" + fileName;
    File file = new File(dest);
    try {
        headerImage.transferTo(file);
    } catch (IOException e) {
        logger.error("上传文件失败: " + e.getMessage());
        throw new RuntimeException("上传文件失败,服务器发生异常!", e);
    }

    User user = hostHolder.getUser();
    String headerUrl = domain + contextPath + "/user/header/" + fileName;
    userService.updateHeader(user.getId(), headerUrl);
    return "redirect:/user/setting";
}
```

读取头像

```java
@RequestMapping(path = "/header/{fileName}")
public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response) {
    // 服务器存放路径
    fileName = uploadPath + "/" + fileName;
    // 文件后缀
    String suffix = fileName.substring(fileName.lastIndexOf(".") + 1);
    // 设置响应类型
    response.setContentType("image/" + suffix);

    // 文件输入输出流
    try (FileInputStream fis = new FileInputStream(fileName);
         OutputStream fos = response.getOutputStream()) {

        byte[] buffer = new byte[1024];
        int b = 0;
        while ((b = fis.read(buffer)) != -1) {
            fos.write(buffer, 0, b);
        }
    } catch (IOException e) {
        logger.error("读取头像失败: " + e.getMessage());
    }
}
```

## 9.2 上传到云服务器

### 9.2.1 前端

```html
<!-- 利用前端代码进行提交 -->
<form class="mt-5" id="uploadForm">
    <div class="form-group row mt-4">
        <label for="head-image" class="col-sm-2 col-form-label text-right">选择头像:</label>
        <div class="col-sm-10">
            <div class="custom-file">
                <input type="hidden" name="token" th:value="${uploadToken}">
                <input type="hidden" name="key" th:value="${fileName}">
                <!--这里name必须写成file-->
                <input type="file" class="custom-file-input" id="head-image" name="file"  lang="es" required="">
                <label class="custom-file-label" for="head-image" data-browse="文件">选择一张图片</label>
                <div class="invalid-feedback">
                    错误啦！
                </div>
            </div>
        </div>
    </div>
    <div class="form-group row mt-4">
        <div class="col-sm-2"></div>
        <div class="col-sm-10 text-center">
            <button type="submit" class="btn btn-info text-white form-control">立即上传</button>
        </div>
    </div>
</form>
```

```js
$(function(){
    $("#uploadForm").submit(upload);
});
// 利用异步提交的方式
function upload() {
    $.ajax({
        url: "http://upload-cn-east-2.qiniup.com",
        method: "post",
        // 不需要把提交的表单转成字符串
        processData: false,
        // 让浏览器自动设置，不可以为true
        contentType: false,
        data: new FormData($("#uploadForm")[0]),
        success: function(data) {
            if(data && data.code == 0) {
                // 更新头像访问路径
                $.post(
                    CONTEXT_PATH + "/user/header/url",
                    {"fileName":$("input[name='key']").val()},
                    function(data) {
                        data = $.parseJSON(data);
                        if(data.code == 0) {
                            window.location.reload();
                        } else {
                            alert(data.msg);
                        }
                    }
                );
            } else {
                alert("上传失败!");
            }
        }
    });
    return false;
}
```

### 9.2.2 后端

```java
@LoginRequired
@RequestMapping(path = "/setting", method = RequestMethod.GET)
public String getSettingPage(Model model) {
    // 文件名称
    String fileName = CommunityUtil.generateUUID();
    // 设置响应信息（七牛云特定的写法模板）
    StringMap policy = new StringMap();
    policy.put("returnBody", CommunityUtil.getJSONString(0));
    // 生成上传凭证
    Auth auth = Auth.create(accessKey, secretKey);
    String uploadToken = auth.uploadToken(headerBucketName, fileName, 3600, policy);

    model.addAttribute("uploadToken", uploadToken);
    model.addAttribute("fileName", fileName);

    return "/site/setting";
}

@RequestMapping(path = "header/url", method = RequestMethod.POST)
@ResponseBody
public String updateHeaderUrl(String fileName) {
    if(StringUtils.isBlank(fileName)) {
        return CommunityUtil.getJSONString(1, "文件名不可以为空");
    }
    String url = headerBucketUrl+"/"+fileName;
    userService.updateHeader(hostHolder.getUser().getId(), url);
    return CommunityUtil.getJSONString(0, "成功！");
}
```

### 9.2.3 本地文件上传至云端

这里UploadTask类通过实现Runnable接口便于启动定时任务上传文件。

```java
/*=========调用============*/
UploadTask task = new UploadTask(fileName, suffix);
// 启动定时器，定时查看是否生成长图成功
// 成功后会返回一个值
Future future = taskScheduler.scheduleAtFixedRate(task, 500);
task.setFuture(future); //修改这个状态就可以停止该线程
/*=========调用============*/

class UploadTask implements Runnable {
    //文件名称
    private String fileName;
    //文件后缀
    private String suffix;
    //启动任务的返回值
    private Future future;
    //开始时间
    private long startTime;
    //上传次数
    private int uploadTimes;

    public void setFuture(Future future) {
        this.future = future;
    }

    public UploadTask(String fileName, String suffix) {
        this.fileName = fileName;
        this.suffix = suffix;
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public void run() {
        // 生成图片失败
        if (System.currentTimeMillis() - startTime > 30000) {
            logger.error("执行时间过长，终止任务：" + fileName);
            future.cancel(true);
            return;
        }
        //上传失败
        if (uploadTimes >= 3) {
            logger.error("上传次数过多，终止任务：" + fileName);
            future.cancel(true);
            return;
        }

        String path = wkImageStorage + "/" + fileName + suffix;
        File file = new File(path);
        if (file.exists()) {
            logger.info(String.format("开始第%d次上传[%s]", ++uploadTimes, fileName));
            /*=================七牛云特定的写法模板======================*/
            // 设置响应信息
            StringMap policy = new StringMap();
            policy.put("returnBody", CommunityUtil.getJSONString(0));
            // 生成上传凭证
            Auth auth = Auth.create(accessKey, secretKey);
            String uploadToken = auth.uploadToken(shareBucketName, fileName, 3600, policy);

            //指定上传机房
            UploadManager manager = new UploadManager(new Configuration(Zone.autoZone()));
            try {
                //开始上传
                Response response = manager.put(
                    path, fileName, uploadToken, null, "image/" + suffix.substring(suffix.lastIndexOf(".") + 1), false
                );
            /*=================七牛云特定的写法模板======================*/
                // 处理响应结果
                JSONObject json = JSONObject.parseObject(response.bodyString());
                if (json == null || json.get("code") == null || !json.get("code").toString().equals("0")) {
                    logger.info(String.format("第%d次上传失败[%s].", uploadTimes, fileName));
                } else {
                    logger.info(String.format("第%d次上传成功[%s].", uploadTimes, fileName));
                    future.cancel(true);
                }
            } catch (QiniuException e) {
                logger.info(String.format("第%d次上传失败[%s].", uploadTimes, fileName));
            }
        } else {
            logger.info("等待图片生成[" + fileName + "].");
        }
    }
}
```

# 10. 登录状态检查（拦截器）

通过自定义一个注解 @LoginRequired，然后编写一个拦截器去检查带有该注解的方法，检验hostholder中是否有用户实例。

不过该方法在运用Spring Security之后就被淘汰了。

```java
//标注注解用于方法，在运行时起作用
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginRequired {
}
```

```java
@Component
public class LoginRequiredInterceptor implements HandlerInterceptor {

    @Autowired
    private HostHolder hostHolder;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if(handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Method method = handlerMethod.getMethod();
            LoginRequired annotation = method.getAnnotation(LoginRequired.class);
            if (annotation != null && hostHolder.getUser() == null) {
                response.sendRedirect(request.getContextPath() + "/login");
                return false;
            }
        }
        return true;
    }
}
```

可以看到，这个拦截器的preHandle方法必须在登录凭证拦截器preHandle方法执行之后才可以执行，那么多个拦截器的时候，如何去安排他们执行的顺序呢？这时候需要在配置类中规定，哪个拦截器先被配置，哪个就先执行。

```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
//
//    @Autowired
//    private AlphaInterceptor alphaInterceptor;

    @Autowired
    private LoginTicketInterceptor loginTicketInterceptor;

    @Autowired
    private LoginRequiredInterceptor loginRequiredInterceptor;

    @Autowired
    private MessageInterceptor messageInterceptor;

    @Autowired
    private DataInterceptor dataInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
//        //排除静态资源
//        //只拦截login register
//        registry.addInterceptor(alphaInterceptor)
//                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg")
//                .addPathPatterns("/register", "/login");

        registry.addInterceptor(loginTicketInterceptor)
                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg");

        registry.addInterceptor(loginRequiredInterceptor)
                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg");

        registry.addInterceptor(messageInterceptor)
                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg");

        registry.addInterceptor(dataInterceptor)
                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg");

    }
}
```

从上面可以总结出各个拦截器方法执行的顺序：

```
loginTicketInterceptor	preHandle
	loginRequiredInterceptor  preHandle
		messageInterceptor	preHandle
			dataInterceptor  preHandle
			dataInterceptor  postHandle
		messageInterceptor  postHandle
	loginRequiredInterceptor postHandle
loginTicketInterceptor postHandle
/*
postHandler在拦截器链内所有拦截器返成功调用
afterCompletion只有preHandle返回true才调用
afterCompletion按拦截器定义逆序调用
*/
```

# 11. 敏感词过滤发布帖子

## 11.1 字典树

### 11.1.1 节点

```java
class TrieNode {

    // 关键词结束标识
    private boolean isKeywordEnd = false;

    // 子节点(key是下级字符,value是下级节点)
    private Map<Character, TrieNode> subNodes = new HashMap<>();

    public boolean isKeywordEnd() {
        return isKeywordEnd;
    }

    public void setKeywordEnd(boolean keywordEnd) {
        isKeywordEnd = keywordEnd;
    }

    // 添加子节点
    public void addSubNode(Character c, TrieNode node) {
        subNodes.put(c, node);
    }

    // 获取子节点
    public TrieNode getSubNode(Character c) {
        return subNodes.get(c);
    }

}
```

### 11.1.2 敏感词过滤器

```java
@Component
public class SensitiveFilter {

    private static final Logger logger = LoggerFactory.getLogger(SensitiveFilter.class);

    private TrieNode rootNode = new TrieNode();

    // 在服务器加载Servlet的时候运行，并且只会被服务器执行一次。在对象加载完依赖注入后执行。
    @PostConstruct
    public void init() {
        // 加载敏感词文件
        try (
                InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(is))
        ) {
            String keyword = "";
            while ((keyword = reader.readLine()) != null) {
                // 每一行就是单独的一个敏感词
                this.addKeyword(keyword);
            }

        } catch (IOException e) {
            logger.error("加载文件失败！：" + e.getMessage());
            throw new IllegalArgumentException(e);
        }
    }


    private void addKeyword(String keyword) {
        TrieNode tempNode = rootNode;
        for (int i = 0; i < keyword.length(); i++) {
            char c = keyword.charAt(i);
            TrieNode subNode = tempNode.getSubNode(c);

            if (subNode == null) {
                // 初始化子节点
                subNode = new TrieNode();
                tempNode.addSubNode(c, subNode);
            }

            // 指向子节点,进入下一轮循环
            tempNode = subNode;

            // 设置结束标识
            if (i == keyword.length() - 1) {
                tempNode.setKeywordEnd(true);
            }
        }
    }

    public String filter(String text) {
        if (StringUtils.isBlank(text)) return null;

        int begin = 0;
        int end = 0;
        TrieNode tempNode = rootNode;
        StringBuilder sb = new StringBuilder();

        // begin指向疑似敏感词的头
        while (begin < text.length()) {

            if (end >= text.length()) {
                // 证明 begin-end 这之间的词不是敏感词，可以把begin加入
                // 从下一个位置开始重新搜寻敏感词
                sb.append(text.charAt(begin++));
                end = begin;
                continue;
            }

            Character c = text.charAt(end);

            // 如果是符号而不是一个字符，应当略过
            if(isSymbol(c)) {
                // 如果是头节点，也就是begin=end
                if (tempNode == rootNode) {
                    sb.append(c);
                    begin++;
                }
                end++;
                continue;
            }

            TrieNode curNode = tempNode.getSubNode(c);
            if(curNode == null) {
                // 以begin开头的字符串不是敏感词，把begin加入，而不是一整个词
                sb.append(text.charAt(begin++));
                end = begin;
                // 进行下一轮搜寻
                tempNode = this.rootNode;
            } else if(!curNode.isKeywordEnd()) {
                // 疑似敏感词，继续向下搜寻
                tempNode = curNode;
                end++;
            } else {
                sb.append("***");
                begin = ++end;
                tempNode = this.rootNode;
            }
        }
        return sb.toString();
    }

    // 判断是否为符号
    private boolean isSymbol(Character c) {
        // 0x2E80~0x9FFF 是东亚文字范围
        return !CharUtils.isAsciiAlphanumeric(c) && (c < 0x2E80 || c > 0x9FFF);
    }
}
```

## 11.2 service

业务层中，在添加帖子的时候就进行过滤

```java
public int addDiscussPost(DiscussPost post) {
    if (post == null) {
        throw new IllegalArgumentException("参数不可为空");
    }

    // 转义HTML标记
    post.setTitle(HtmlUtils.htmlEscape(post.getTitle()));
    post.setContent(HtmlUtils.htmlEscape(post.getContent()));

    // 敏感词过滤标题和内容
    post.setTitle(sensitiveFilter.filter(post.getTitle()));
    post.setContent(sensitiveFilter.filter(post.getContent()));

    // 最后调用dao层加入到数据库中
    return discussPostMapper.insertDiscussPost(post);
}
```

## 11.3 controller

```java
@RequestMapping(path = "/add", method = RequestMethod.POST)
@ResponseBody
public String addDiscussPost(String title, String content) {
    User user = hostHolder.getUser();
    if (user == null) {
        return CommunityUtil.getJSONString(403, "请登录后再发帖！");
    }

    DiscussPost post = new DiscussPost();
    post.setUserId(user.getId());
    post.setTitle(title);
    post.setContent(content);
    post.setCreateTime(new Date());

    discussPostService.addDiscussPost(post);

    //发送事件
    Event event = new Event()
        .setTopic(TOPIC_PUBLISH)
        .setEntityUserId(user.getId())
        .setEntityType(ENTITY_TYPE_POST)
        .setEntityId(post.getId());
    eventProducer.fileEvent(event);

    //计算帖子分数
    //加入redis库中，这个索引利用quartz绑定了定时操作
    //会定时对于该索引里存储的所有帖子进行分数更新
    String redisKey = RedisKeyUtil.getPostScoreKey();
    redisTemplate.opsForSet().add(redisKey, post.getId());

    return CommunityUtil.getJSONString(0, "发布成功！");
}
```

## 11.4 Kafka消费发帖事件

```java
@KafkaListener(topics = {TOPIC_PUBLISH})
public void handlePublishMessage(ConsumerRecord record) {
    if (record == null || record.value() == null) {
        logger.error("消息的内容为空!");
        return;
    }

    Event event = JSONObject.parseObject(record.value().toString(), Event.class);
    if (event == null) {
        logger.error("消息格式错误!");
        return;
    }

    // 调用业务层发布帖子
    DiscussPost post = discussPostService.findDiscssPost(event.getEntityId());
    // 更新搜索引擎库
    elasticsearchService.saveDiscussPost(post);
}
```

## 11.5 前端异步发送帖子

使用jQuery发送AJAX请求。

```js
$(function(){
	$("#publishBtn").click(publish);
});

function publish() {
	$("#publishModal").modal("hide");

	// 获取标题和内容
	var title = $("#recipient-name").val();
	var content = $("#message-text").val();
	// 发送异步请求(POST)
	$.post(
		CONTEXT_PATH + "/discuss/add",
		{"title":title,"content":content},
		function(data) {
			data = $.parseJSON(data);
			// 在提示框中显示返回消息
			$("#hintBody").text(data.msg);
			// 显示提示框
			$("#hintModal").modal("show");
			// 2秒后,自动隐藏提示框
			setTimeout(function(){
				$("#hintModal").modal("hide");
				// 刷新页面
				if(data.code == 0) {
					window.location.reload();
				}
			}, 2000);
		}
	);

}
```

