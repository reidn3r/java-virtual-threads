package com.github.reidn3r.async_multithreading.domain;

import java.sql.Date;

import org.springframework.data.annotation.CreatedDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Entity
@Data
@Table(
  name = "tb_likes",
  uniqueConstraints = @UniqueConstraint(
    name = "unique_user_post_like",
    columnNames = {"userId", "postId"}
  )
)
public class LikeEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long userId;

  private Long postId;

  @CreatedDate
  private Date created_at;
}
