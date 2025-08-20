TRUNCATE TABLE tb_likes, tb_shares, tb_posts RESTART IDENTITY CASCADE;

-- Post inicial
INSERT INTO tb_posts (content, url, user_id, likes_count, shares_count)
VALUES ('random_content', 'https://url1.com', 0, 0, 0);

-- Mais 15 Posts
INSERT INTO tb_posts (content, url, user_id, likes_count, shares_count) VALUES 
('Explorando novas tecnologias em 2025', 'https://techblog1.com', 1, 0, 0),
('Como cultivar hábitos de estudo eficientes', 'https://studyzone.com', 2, 0, 0),
('Top 10 filmes de ficção científica', 'https://scifimovies.net', 3, 0, 0),
('Receita prática de pão caseiro', 'https://cookeasy.org', 4, 0, 0),
('Viagem inesquecível para o Japão', 'https://traveljapan.jp', 5, 0, 0),
('Dicas para organizar suas finanças pessoais', 'https://financesimple.com', 6, 0, 0),
('Guia de treinos para iniciantes na academia', 'https://fitstart.co', 7, 0, 0),
('Reflexões sobre inteligência artificial', 'https://aithoughts.ai', 8, 0, 0),
('Como montar um PC gamer em 2025', 'https://pcbuildhub.com', 9, 0, 0),
('Os melhores cafés especiais do Brasil', 'https://cafebrasil.br', 10, 0, 0),
('Tutorial completo de Spring Boot', 'https://springboot.dev', 11, 0, 0),
('Tendências de moda sustentável', 'https://greenstyle.org', 12, 0, 0),
('Impacto da música no bem-estar', 'https://musiclife.fm', 13, 0, 0),
('Aprendendo fotografia com smartphone', 'https://photoacademy.io', 14, 0, 0),
('Jogos indie que você precisa conhecer', 'https://indiegames.gg', 15, 0, 0);
