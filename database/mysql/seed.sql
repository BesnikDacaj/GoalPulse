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
