INSERT INTO league_tier (tier_name) VALUES ('First Division');
INSERT INTO league (name, country, current_season, tier_id) VALUES
('Premier League', 'England', '2025/26', 1),
('La Liga', 'Spain', '2025/26', 1),
('Bundesliga', 'Germany', '2025/26', 1);

INSERT INTO season (league_id, name, start_date, end_date) VALUES
(1, '2025/26', '2025-08-15', '2026-05-24'),
(2, '2025/26', '2025-08-16', '2026-05-24'),
(3, '2025/26', '2025-08-22', '2026-05-16');

INSERT INTO team (league_id, name, short_name, stadium, city, coach_name, founded_year) VALUES
(1, 'Manchester City', 'MCI', 'Etihad Stadium', 'Manchester', 'Pep Guardiola', 1880),
(1, 'Liverpool', 'LIV', 'Anfield', 'Liverpool', 'Arne Slot', 1892),
(2, 'Real Madrid', 'RMA', 'Santiago Bernabeu', 'Madrid', 'Carlo Ancelotti', 1902),
(2, 'Barcelona', 'BAR', 'Camp Nou', 'Barcelona', 'Hansi Flick', 1899),
(3, 'Bayern Munich', 'BAY', 'Allianz Arena', 'Munich', 'Vincent Kompany', 1900),
(3, 'Borussia Dortmund', 'BVB', 'Signal Iduna Park', 'Dortmund', 'Niko Kovac', 1909);

INSERT INTO player_position (pos_name, pos_code) VALUES
('Goalkeeper', 'GK'), ('Defender', 'DEF'), ('Midfielder', 'MID'), ('Forward', 'FWD');

INSERT INTO player (team_id, position_id, first_name, last_name, nationality, date_of_birth, shirt_number) VALUES
(1, 4, 'Erling', 'Haaland', 'Norway', '2000-07-21', 9),
(1, 3, 'Kevin', 'De Bruyne', 'Belgium', '1991-06-28', 17),
(2, 4, 'Mohamed', 'Salah', 'Egypt', '1992-06-15', 11),
(2, 2, 'Virgil', 'van Dijk', 'Netherlands', '1991-07-08', 4),
(3, 4, 'Vinicius', 'Junior', 'Brazil', '2000-07-12', 7),
(4, 4, 'Lamine', 'Yamal', 'Spain', '2007-07-13', 19),
(5, 4, 'Harry', 'Kane', 'England', '1993-07-28', 9),
(6, 3, 'Julian', 'Brandt', 'Germany', '1996-05-02', 10);

INSERT INTO `match` (season_id, home_team_id, away_team_id, match_date, status, home_score, away_score, venue, referee) VALUES
(1, 1, 2, '2026-04-24 20:45:00', 'live', 2, 1, 'Etihad Stadium', 'Michael Oliver'),
(2, 3, 4, '2026-04-25 21:00:00', 'scheduled', 0, 0, 'Santiago Bernabeu', 'Jose Sanchez'),
(3, 5, 6, '2026-04-20 18:30:00', 'finished', 3, 2, 'Allianz Arena', 'Felix Zwayer');

INSERT INTO match_event (match_id, player_id, event_type, minute, detail) VALUES
(1, 1, 'goal', 18, 'Left-foot finish from inside the box'),
(1, 3, 'goal', 37, 'Penalty converted'),
(1, 2, 'goal', 61, 'Free kick into the top corner'),
(3, 7, 'goal', 12, 'Header from corner'),
(3, 8, 'goal', 44, 'Long-range shot');

INSERT INTO role (role_name, description) VALUES
('guest', 'Read-only public role'),
('registered', 'Personal dashboard, favorites, notifications, exports'),
('admin', 'Full data administration and audit access');

-- Demo users (BCrypt hashes of the documented demo passwords).
-- 'Admin123!'  -> $2a$10$7s7l8gO4sR0K5N1y0D1d3eqW8m1aKjA9r2bC5xV7jO0n5h0p2l3xy
-- 'User123!'   -> $2a$10$P6Yk8eXh1aQv3JiV1pH1uOXq7nTjE0a1Hx2C9yWdKx5n5y7c7YqkO
-- Demo only: in production, generate hashes with PasswordHasher.hash() at runtime.
INSERT INTO app_user (role_id, username, email, password_hash, salt) VALUES
(3, 'admin',     'admin@goalpulse.test', '$2a$10$7s7l8gO4sR0K5N1y0D1d3eqW8m1aKjA9r2bC5xV7jO0n5h0p2l3xy', NULL),
(2, 'mia',       'mia@goalpulse.test',   '$2a$10$P6Yk8eXh1aQv3JiV1pH1uOXq7nTjE0a1Hx2C9yWdKx5n5y7c7YqkO', NULL),
(2, 'noah',      'noah@goalpulse.test',  '$2a$10$P6Yk8eXh1aQv3JiV1pH1uOXq7nTjE0a1Hx2C9yWdKx5n5y7c7YqkO', NULL);

INSERT INTO user_favorite (user_id, team_id) VALUES
(2, 2),
(2, 3),
(3, 5);

INSERT INTO notification (user_id, match_id, event_type, message, is_read) VALUES
(2, 1, 'score',  'Manchester City 2-1 Liverpool — 61'' De Bruyne free kick.', FALSE),
(2, 2, 'start',  'Real Madrid vs Barcelona kicks off in 30 minutes.',          FALSE),
(3, 3, 'final',  'Bayern Munich 3-2 Borussia Dortmund — full time.',           TRUE);
