package com.nowcoder.community.controller;

import com.nowcoder.community.Event.EventProducer;
import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@Controller
@RequestMapping("/comment")
public class CommentController implements CommunityConstant {
    @Autowired
    private CommentService commentService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private DiscussPostService discussPostService;

    @RequestMapping(path = "/add/{discussPostId}", method = RequestMethod.POST)
    public String addComment(@PathVariable("discussPostId") int discussPostId, Comment comment) {
        User user = hostHolder.getUser();
        comment.setUserId(user.getId());
        comment.setStatus(0);
        comment.setCreateTime(new Date());

        commentService.addComment(comment);

        Event event = new Event()
                .setTopic(TOPIC_COMMENT)
                .setUserId(user.getId())
                .setEntityType(comment.getEntityType())
                .setEntityId(comment.getEntityId())
                .setData("postId", discussPostId);
        if(comment.getEntityType() == ENTITY_TYPE_POST) {
            DiscussPost target = discussPostService.findDiscssPost(discussPostId);
            event.setEntityUserId(target.getUserId());
        } else {
            Comment target = commentService.findCommentById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        }
        eventProducer.fileEvent(event);

        return "redirect:/discuss/detail/" + discussPostId;
    }
}
