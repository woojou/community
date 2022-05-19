package com.nowcoder.community.controller;


import com.nowcoder.community.Event.EventProducer;
import com.nowcoder.community.entity.*;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import com.nowcoder.community.util.RedisKeyUtil;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

@Controller
@RequestMapping("/discuss")
public class DiscussPostController implements CommunityConstant {

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private UserService userService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private LikeService likeService;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private RedisTemplate redisTemplate;

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
        String redisKey = RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey, post.getId());

        return CommunityUtil.getJSONString(0, "发布成功！");
    }

    @RequestMapping(path = "/detail/{discussPostId}", method = RequestMethod.GET)
    public String getDiscussPost(@PathVariable("discussPostId") int discussPostId, Model model, Page page) {
        //帖子
        DiscussPost post = discussPostService.findDiscssPost(discussPostId);
        model.addAttribute("post", post);
        //作者
        User user = userService.findUserById(post.getUserId());
        model.addAttribute("user", user);
        //点赞
        User host = hostHolder.getUser();
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, discussPostId);
        model.addAttribute("likeCount", likeCount);
        //注意用户未登录的时候直接设为0
        int likeStatus = host == null ? 0 :
                likeService.findEntityLikeStatus(host.getId(), ENTITY_TYPE_POST, discussPostId);
        model.addAttribute("likeStatus", likeStatus);


        //评论的分页信息
        page.setPath("/discuss/detail/" + discussPostId);
        page.setLimit(5);
        page.setRows(post.getCommentCount()); //在discuss_post表中存储了一个冗余数据保存评论数量

        //当前帖子的所有的评论
        List<Comment> comments = commentService.
                findCommentsByEntity(ENTITY_TYPE_POST, discussPostId, page.getOffset(), page.getLimit());

        // 评论VO列表
        List<Map<String, Object>> commentVoList = new ArrayList<>();

        if (comments != null) {
            for (Comment comment : comments) {
                Map<String, Object> commentVo = new HashMap<>();
                commentVo.put("comment", comment);
                commentVo.put("user", userService.findUserById(comment.getUserId()));
                List<Comment> replys = commentService.
                        findCommentsByEntity(ENTITY_TYPE_REPLY, comment.getId(), 0, Integer.MAX_VALUE);
                List<Map<String, Object>> replyVoList = new ArrayList<>();
                if (replys != null) {
                    for (Comment reply : replys) {
                        Map<String, Object> replyVo = new HashMap<>();
                        // 回复
                        replyVo.put("reply", reply);
                        // 作者
                        replyVo.put("user", userService.findUserById(reply.getUserId()));
                        // 回复目标
                        User target = reply.getTargetId() == 0 ? null : userService.findUserById(reply.getTargetId());
                        replyVo.put("target", target);
                        // 点赞
                        likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_REPLY, reply.getId());
                        replyVo.put("likeCount", likeCount);
                        likeStatus = host == null ? 0 :
                                likeService.findEntityLikeStatus(host.getId(), ENTITY_TYPE_REPLY, reply.getId());
                        replyVo.put("likeStatus", likeStatus);

                        replyVoList.add(replyVo);
                    }
                }
                commentVo.put("replys", replyVoList);
                int replyCount = commentService.findCommentCount(ENTITY_TYPE_REPLY, comment.getId());
                //回复的条数
                commentVo.put("replyCount", replyCount);
                //点赞
                likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_REPLY, comment.getId());
                commentVo.put("likeCount", likeCount);
                likeStatus = host == null ? 0 :
                        likeService.findEntityLikeStatus(host.getId(), ENTITY_TYPE_REPLY, comment.getId());
                commentVo.put("likeStatus", likeStatus);

                commentVoList.add(commentVo);
            }
        }

        model.addAttribute("comments", commentVoList);

        return "/site/discuss-detail";
    }

    //置顶
    @RequestMapping(path = "/top", method = RequestMethod.POST)
    @ResponseBody
    public String setTop(int id) {
        DiscussPost discussPostById = discussPostService.findDiscssPost(id);
        // 获取置顶状态，1为置顶，0为正常状态,1^1=0 0^1=1
        int type = discussPostById.getType()^1;
        discussPostService.updateType(id, type);
        // 返回的结果
        Map<String, Object> map = new HashMap<>();
        map.put("type", type);

        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setEntityUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fileEvent(event);

        return CommunityUtil.getJSONString(0);
    }

    //加精
    @RequestMapping(path = "/wonderful", method = RequestMethod.POST)
    @ResponseBody
    public String setWonderful(int id) {
        DiscussPost discussPostById = discussPostService.findDiscssPost(id);
        int status = discussPostById.getStatus()^1;
        // 1为加精，0为正常， 1^1=0, 0^1=1
        discussPostService.updateStatus(id, status);
        // 返回的结果
        Map<String, Object> map = new HashMap<>();
        map.put("status", status);

        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setEntityUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fileEvent(event);

        return CommunityUtil.getJSONString(0);
    }

    @RequestMapping(path = "/delete", method = RequestMethod.POST)
    @ResponseBody
    public String setDelete(int id) {
        discussPostService.updateStatus(id, 2);

        Event event = new Event()
                .setTopic(TOPIC_DELETE)
                .setEntityUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fileEvent(event);

        return CommunityUtil.getJSONString(0);
    }
}
