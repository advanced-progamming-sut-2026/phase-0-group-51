CREATE TABLE IF NOT EXISTS users (
     id                INTEGER PRIMARY KEY AUTOINCREMENT,
     username          TEXT UNIQUE NOT NULL,
     email             TEXT UNIQUE NOT NULL,
     password_hash     TEXT NOT NULL,
     gender            TEXT,
     nickname          TEXT,
     security_question TEXT,
     answer            TEXT,
     coins             INTEGER DEFAULT 0,
     gems              INTEGER DEFAULT 0,
     seed_packet       INTEGER DEFAULT 0,
     plant_food_num    INTEGER DEFAULT 0,
     most_meow_point   INTEGER DEFAULT 0,
     max_point         INTEGER DEFAULT 0,
     games_played      INTEGER DEFAULT 0,
     mini_games_played INTEGER DEFAULT 0,
     last_won_game     TEXT,
     created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_progress (
                                             user_id       INTEGER PRIMARY KEY REFERENCES users(id),
    chapter_index INTEGER DEFAULT 0,
    level_index   INTEGER DEFAULT 0
    );

CREATE TABLE IF NOT EXISTS user_plants (
                                           user_id     INTEGER REFERENCES users(id),
    plant_id    INTEGER,
    plant_level INTEGER DEFAULT 1,
    PRIMARY KEY (user_id, plant_id)
    );

CREATE TABLE IF NOT EXISTS user_unlocked_plants (
    user_id  INTEGER REFERENCES users(id),
    plant_id INTEGER,
    PRIMARY KEY (user_id, plant_id)
    );

CREATE TABLE IF NOT EXISTS user_scores (
                                           user_id       INTEGER REFERENCES users(id),
    chapter_index INTEGER,
    level_index   INTEGER,
    score         INTEGER DEFAULT 0,
    stars         INTEGER DEFAULT 0,
    PRIMARY KEY (user_id, chapter_index, level_index)
    );
CREATE TABLE IF NOT EXISTS quests (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT UNIQUE NOT NULL,
    condition TEXT NOT NULL,
    priority TEXT NOT NULL,      -- ذخیره نام اینام (CRITICAL, HIGH, ...)
    reward_amount INTEGER NOT NULL,
    reward_type TEXT NOT NULL,   -- ذخیره نام اینام (CURRENCY_COINS, ...)
    quest_type TEXT NOT NULL     -- ذخیره نام اینام (DAILY, MAIN, EPIC)
);
CREATE TABLE IF NOT EXISTS user_quests (
                                           user_id      INTEGER REFERENCES users(id),
    quest_id     INTEGER,
    is_daily     INTEGER DEFAULT 0,
    is_completed INTEGER DEFAULT 0,
    reset_date   TEXT,
    PRIMARY KEY (user_id, quest_id)
    );

CREATE TABLE IF NOT EXISTS user_minigames (
    user_id      INTEGER REFERENCES users(id),
    minigame_id  INTEGER,
    high_score   INTEGER DEFAULT 0,
    times_played INTEGER DEFAULT 0,
    PRIMARY KEY (user_id, minigame_id)
    );
CREATE TABLE IF NOT EXISTS news (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE IF NOT EXISTS user_news (
    user_id INTEGER REFERENCES users(id),
    news_id INTEGER REFERENCES news(id),
    is_read INTEGER DEFAULT 0, -- 0 یعنی نخوانده، 1 یعنی خوانده شده
    PRIMARY KEY (user_id, news_id)
);