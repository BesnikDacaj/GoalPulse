CREATE TABLE league_tier (
  tier_id INT AUTO_INCREMENT PRIMARY KEY,
  tier_name VARCHAR(80) UNIQUE NOT NULL
);

CREATE TABLE league (
  league_id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  country VARCHAR(80) NOT NULL,
  logo_url VARCHAR(255),
  current_season VARCHAR(20),
  tier_id INT,
  FOREIGN KEY (tier_id) REFERENCES league_tier(tier_id) ON DELETE SET NULL
);

CREATE TABLE season (
  season_id INT AUTO_INCREMENT PRIMARY KEY,
  league_id INT NOT NULL,
  name VARCHAR(30) NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  FOREIGN KEY (league_id) REFERENCES league(league_id) ON DELETE CASCADE
);

CREATE TABLE team (
  team_id INT AUTO_INCREMENT PRIMARY KEY,
  league_id INT NOT NULL,
  name VARCHAR(120) NOT NULL,
  short_name VARCHAR(12) NOT NULL,
  logo_url VARCHAR(255),
  stadium VARCHAR(120),
  city VARCHAR(80),
  coach_name VARCHAR(120),
  founded_year INT,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  CHECK (founded_year >= 1850),
  FOREIGN KEY (league_id) REFERENCES league(league_id) ON DELETE RESTRICT
);

CREATE TABLE player_position (
  position_id INT AUTO_INCREMENT PRIMARY KEY,
  pos_name VARCHAR(40) UNIQUE NOT NULL,
  pos_code VARCHAR(3) UNIQUE NOT NULL
);

CREATE TABLE player (
  player_id INT AUTO_INCREMENT PRIMARY KEY,
  team_id INT,
  position_id INT,
  first_name VARCHAR(80) NOT NULL,
  last_name VARCHAR(80) NOT NULL,
  nationality VARCHAR(80),
  date_of_birth DATE,
  shirt_number INT,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  CHECK (shirt_number BETWEEN 1 AND 99),
  FOREIGN KEY (team_id) REFERENCES team(team_id) ON DELETE SET NULL,
  FOREIGN KEY (position_id) REFERENCES player_position(position_id) ON DELETE SET NULL
);

CREATE TABLE `match` (
  match_id INT AUTO_INCREMENT PRIMARY KEY,
  season_id INT NOT NULL,
  home_team_id INT NOT NULL,
  away_team_id INT NOT NULL,
  match_date DATETIME NOT NULL,
  status VARCHAR(20) NOT NULL,
  home_score INT NOT NULL DEFAULT 0,
  away_score INT NOT NULL DEFAULT 0,
  venue VARCHAR(120),
  referee VARCHAR(120),
  CHECK (status IN ('scheduled','live','finished')),
  CHECK (home_score >= 0),
  CHECK (away_score >= 0),
  CHECK (home_team_id <> away_team_id),
  FOREIGN KEY (season_id) REFERENCES season(season_id) ON DELETE CASCADE,
  FOREIGN KEY (home_team_id) REFERENCES team(team_id) ON DELETE RESTRICT,
  FOREIGN KEY (away_team_id) REFERENCES team(team_id) ON DELETE RESTRICT
);

CREATE TABLE match_event (
  event_id INT AUTO_INCREMENT PRIMARY KEY,
  match_id INT NOT NULL,
  player_id INT,
  event_type VARCHAR(30) NOT NULL,
  minute INT NOT NULL,
  detail VARCHAR(255),
  CHECK (minute BETWEEN 0 AND 130),
  FOREIGN KEY (match_id) REFERENCES `match`(match_id) ON DELETE CASCADE,
  FOREIGN KEY (player_id) REFERENCES player(player_id) ON DELETE SET NULL
);

CREATE TABLE match_lineup (
  lineup_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  match_id INT NOT NULL,
  player_id INT NOT NULL,
  is_starter BOOLEAN NOT NULL DEFAULT TRUE,
  UNIQUE (match_id, player_id),
  FOREIGN KEY (match_id) REFERENCES `match`(match_id) ON DELETE CASCADE,
  FOREIGN KEY (player_id) REFERENCES player(player_id) ON DELETE CASCADE
);

CREATE TABLE standing (
  standing_id INT AUTO_INCREMENT PRIMARY KEY,
  season_id INT NOT NULL,
  team_id INT NOT NULL,
  played INT NOT NULL DEFAULT 0,
  won INT NOT NULL DEFAULT 0,
  drawn INT NOT NULL DEFAULT 0,
  lost INT NOT NULL DEFAULT 0,
  goals_for INT NOT NULL DEFAULT 0,
  goals_against INT NOT NULL DEFAULT 0,
  points INT NOT NULL DEFAULT 0,
  UNIQUE (season_id, team_id),
  FOREIGN KEY (season_id) REFERENCES season(season_id) ON DELETE CASCADE,
  FOREIGN KEY (team_id) REFERENCES team(team_id) ON DELETE CASCADE
);

CREATE TABLE role (
  role_id INT AUTO_INCREMENT PRIMARY KEY,
  role_name VARCHAR(30) UNIQUE NOT NULL,
  description VARCHAR(255)
);

CREATE TABLE app_user (
  user_id INT AUTO_INCREMENT PRIMARY KEY,
  role_id INT NOT NULL,
  username VARCHAR(60) UNIQUE NOT NULL,
  email VARCHAR(120) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  salt VARCHAR(80),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  FOREIGN KEY (role_id) REFERENCES role(role_id) ON DELETE RESTRICT
);

CREATE TABLE user_favorite (
  favorite_id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  team_id INT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (user_id, team_id),
  FOREIGN KEY (user_id) REFERENCES app_user(user_id) ON DELETE CASCADE,
  FOREIGN KEY (team_id) REFERENCES team(team_id) ON DELETE CASCADE
);

CREATE TABLE notification (
  notif_id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  match_id INT,
  event_type VARCHAR(30) NOT NULL,
  message VARCHAR(255) NOT NULL,
  is_read BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES app_user(user_id) ON DELETE CASCADE,
  FOREIGN KEY (match_id) REFERENCES `match`(match_id) ON DELETE CASCADE
);

CREATE TABLE audit_log (
  log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id INT,
  action VARCHAR(20) NOT NULL,
  table_name VARCHAR(60) NOT NULL,
  record_id VARCHAR(60),
  old_value TEXT,
  new_value TEXT,
  timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ip_address VARCHAR(60),
  FOREIGN KEY (user_id) REFERENCES app_user(user_id) ON DELETE SET NULL
);
