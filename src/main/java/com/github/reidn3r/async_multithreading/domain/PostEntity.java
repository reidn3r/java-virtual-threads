package com.github.reidn3r.async_multithreading.domain;


import java.sql.Date;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name="tb_posts")
public class PostEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long userId;
  private Long postId;
  private String content;
  private String url;
  private Long likes_count;
  private Long shares_count;

  @CreatedDate
  private Date created_at;

  @OneToMany(mappedBy = "posts")
  private List<LikeEntity> likes;

  @OneToMany(mappedBy = "posts")
  private List<ShareEntity> shares;
}
