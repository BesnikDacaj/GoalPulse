CREATE TABLE league_tier (
  tier_id SERIAL PRIMARY KEY,
  tier_name VARCHAR(80) UNIQUE NOT NULL
);

CREATE TABLE league (
  league_id SERIAL PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  country VARCHAR(80) NOT NULL,
  logo_url VARCHAR(255),
  current_season VARCHAR(20),
  tier_id INT REFERENCES league_tier(tier_id) ON DELETE SET NULL
);

CREATE TABLE season (
  season_id SERIAL PRIMARY KEY,
  league_id INT NOT NULL REFERENCES league(league_id) ON DELETE CASCADE,
  name VARCHAR(30) NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL
);

CREATE TABLE team (
  team_id SERIAL PRIMARY KEY,
  league_id INT NOT NULL REFERENCES league(league_id) ON DELETE RESTRICT,
  name VARCHAR(120) NOT NULL,
  short_name VARCHAR(12) NOT NULL,
  logo_url VARCHAR(255),
  stadium VARCHAR(120),
  city VARCHAR(80),
  coach_name VARCHAR(120),
  founded_year INT CHECK (founded_year >= 1850),
  is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE player_position (
  position_id SERIAL PRIMARY KEY,
  pos_name VARCHAR(40) UNIQUE NOT NULL,
  pos_code VARCHAR(3) UNIQUE NOT NULL
);

CREATE TABLE player (
  player_id SERIAL PRIMARY KEY,
  team_id INT REFERENCES team(team_id) ON DELETE SET NULL,
  position_id INT REFERENCES player_position(position_id) ON DELETE SET NULL,
  first_name VARCHAR(80) NOT NULL,
  last_name VARCHAR(80) NOT NULL,
  nationality VARCHAR(80),
  date_of_birth DATE,
  shirt_number INT CHECK (shirt_number BETWEEN 1 AND 99),
  is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE match (
  match_id SERIAL PRIMARY KEY,
  season_id INT NOT NULL REFERENCES season(season_id) ON DELETE CASCADE,
  home_team_id INT NOT NULL REFERENCES team(team_id) ON DELETE RESTRICT,
  away_team_id INT NOT NULL REFERENCES team(team_id) ON DELETE RESTRICT,
  match_date TIMESTAMP NOT NULL,
  status VARCHAR(20) NOT NULL CHECK (status IN ('scheduled','live','finished')),
  home_score INT NOT NULL DEFAULT 0 CHECK (home_score >= 0),
  away_score INT NOT NULL DEFAULT 0 CHECK (away_score >= 0),
  venue VARCHAR(120),
  referee VARCHAR(120),
  CHECK (home_team_id <> away_team_id)
);

CREATE TABLE match_event (
  event_id SERIAL PRIMARY KEY,
  match_id INT NOT NULL REFERENCES match(match_id) ON DELETE CASCADE,
  player_id INT REFERENCES player(player_id) ON DELETE SET NULL,
  event_type VARCHAR(30) NOT NULL,
  minute INT NOT NULL CHECK (minute BETWEEN 0 AND 130),
  detail VARCHAR(255)
);

CREATE TABLE match_lineup (
  lineup_id BIGSERIAL PRIMARY KEY,
  match_id INT NOT NULL REFERENCES match(match_id) ON DELETE CASCADE,
  player_id INT NOT NULL REFERENCES player(player_id) ON DELETE CASCADE,
  is_starter BOOLEAN NOT NULL DEFAULT TRUE,
  UNIQUE (match_id, player_id)
);

CREATE TABLE standing (
  standing_id SERIAL PRIMARY KEY,
  season_id INT NOT NULL REFERENCES season(season_id) ON DELETE CASCADE,
  team_id INT NOT NULL REFERENCES team(team_id) ON DELETE CASCADE,
  played INT NOT NULL DEFAULT 0,
  won INT NOT NULL DEFAULT 0,
  drawn INT NOT NULL DEFAULT 0,
  lost INT NOT NULL DEFAULT 0,
  goals_for INT NOT NULL DEFAULT 0,
  goals_against INT NOT NULL DEFAULT 0,
  points INT NOT NULL DEFAULT 0,
  UNIQUE (season_id, team_id)
);

CREATE TABLE role (
  role_id SERIAL PRIMARY KEY,
  role_name VARCHAR(30) UNIQUE NOT NULL,
  description VARCHAR(255)
);

CREATE TABLE app_user (
  user_id SERIAL PRIMARY KEY,
  role_id INT NOT NULL REFERENCES role(role_id) ON DELETE RESTRICT,
  username VARCHAR(60) UNIQUE NOT NULL,
  email VARCHAR(120) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  salt VARCHAR(80),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE user_favorite (
  favorite_id SERIAL PRIMARY KEY,
  user_id INT NOT NULL REFERENCES app_user(user_id) ON DELETE CASCADE,
  team_id INT NOT NULL REFERENCES team(team_id) ON DELETE CASCADE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (user_id, team_id)
);

CREATE TABLE notification (
  notif_id SERIAL PRIMARY KEY,
  user_id INT NOT NULL REFERENCES app_user(user_id) ON DELETE CASCADE,
  match_id INT REFERENCES match(match_id) ON DELETE CASCADE,
  event_type VARCHAR(30) NOT NULL,
  message VARCHAR(255) NOT NULL,
  is_read BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE audit_log (
  log_id BIGSERIAL PRIMARY KEY,
  user_id INT REFERENCES app_user(user_id) ON DELETE SET NULL,
  action VARCHAR(20) NOT NULL,
  table_name VARCHAR(60) NOT NULL,
  record_id VARCHAR(60),
  old_value TEXT,
  new_value TEXT,
  timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ip_address VARCHAR(60)
);
