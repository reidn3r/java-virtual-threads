package com.github.reidn3r.async_multithreading.config;

import com.github.reidn3r.async_multithreading.domain.PostEntity;
import com.github.reidn3r.async_multithreading.repository.PostsRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final PostsRepository postRepository;
    
    private static final String[] CONTENTS = {
        "Hoje foi um dia incrível! Aprendi algo novo sobre programação.",
        "Compartilhando algumas dicas de produtividade que tenho usado.",
        "Acabei de terminar um projeto importante. Que sensação boa!",
        "Refletindo sobre as últimas semanas e planejando o futuro.",
        "Dica rápida: sempre faça backup dos seus arquivos importantes!",
        "Conheci um lugar novo hoje. As experiências enriquecem a vida.",
        "Estudando Spring Boot e suas maravilhas. Recomendo!",
        "Momento de descontração: qual foi a última série que vocês assistiram?",
        "Compartilhando um pouco do meu processo criativo.",
        "A importância de manter uma rotina saudável no dia a dia.",
        "Desafio do dia: aprender uma skill nova em uma semana.",
        "As pequenas vitórias também merecem celebração!",
        "Dica de livro: tem algum que mudou sua perspectiva recentemente?",
        "Trabalhando remoto tem seus desafios, mas também muitas vantagens.",
        "Momento tech: quais frameworks vocês estão usando atualmente?",
        "A natureza sempre tem lições para nos ensinar.",
        "Compartilhando um pouco da minha jornada de aprendizado.",
        "A gratidão transforma o que temos em suficiente.",
        "Dica de carreira: networking é fundamental!",
        "Cada dia é uma nova oportunidade para recomeçar."
    };

    private static final String[] URLS = {
        "https://example.com/post/",
        "https://myblog.com/article/",
        "https://socialplatform.com/share/",
        "https://media.site.com/content/",
        "https://resources.example.com/blog/",
        "https://platform.net/posts/",
        "https://content.hub.com/entry/",
        "https://share.website.com/item/",
        "https://blog.service.com/post/",
        "https://media.portal.com/content/"
    };

    @Override
    public void run(String... args) throws Exception {
        if (postRepository.count() == 0) {
            System.out.println("Iniciando carga de dados...");
            loadPosts();
            System.out.println("Carga de dados concluída!");
        }
    }

    private void loadPosts() {
        List<PostEntity> posts = new ArrayList<>();
        Random random = new Random();
        
        for (int i = 1; i <= 500; i++) {
            PostEntity post = new PostEntity();
            
            // Gera dados aleatórios
            post.setUserId((long) i); // User IDs entre 1 e 100
            post.setContent(generateRandomContent());
            post.setUrl(generateRandomUrl(i));
            post.setLikes_count((long) 0); // Até 1000 likes
            post.setShares_count((long) 0); // Até 500 shares
            post.setCreated_at(generateRandomDate());
            
            // Listas vazias (como solicitado)
            post.setLikes(new ArrayList<>());
            post.setShares(new ArrayList<>());
            
            posts.add(post);
            
            // Salva em lotes para melhor performance
            if (posts.size() % 100 == 0) {
                postRepository.saveAll(posts);
                posts.clear();
                System.out.println("Salvou lote de 100 posts...");
            }
        }
        
        // Salva os posts restantes
        if (!posts.isEmpty()) {
            postRepository.saveAll(posts);
        }
    }

    private String generateRandomContent() {
        Random random = new Random();
        return CONTENTS[random.nextInt(CONTENTS.length)] + 
               " #post" + (random.nextInt(1000) + 1);
    }

    private String generateRandomUrl(int postId) {
        Random random = new Random();
        return URLS[random.nextInt(URLS.length)] + postId + 
               "-" + System.currentTimeMillis();
    }

    private Date generateRandomDate() {
        // Gera datas aleatórias nos últimos 365 dias
        long minDay = LocalDate.now().minusDays(365).toEpochDay();
        long maxDay = LocalDate.now().toEpochDay();
        long randomDay = ThreadLocalRandom.current().nextLong(minDay, maxDay);
        
        return Date.valueOf(LocalDate.ofEpochDay(randomDay));
    }
}