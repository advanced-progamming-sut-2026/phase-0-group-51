CREATE TABLE IF NOT EXISTS users (
     id                INTEGER PRIMARY KEY AUTOINCREMENT,
     username          TEXT UNIQUE NOT NULL,
     email             TEXT UNIQUE NOT NULL,
     password_hash     TEXT NOT NULL,
     gender            TEXT,
     nickname          TEXT,
     security_question INTEGER,
     answer            TEXT,
     coins             INTEGER DEFAULT 0,
     gems              INTEGER DEFAULT 0,
     seed_packet       INTEGER DEFAULT 0,
     plant_food_num    INTEGER DEFAULT 0,
     most_meow_point   INTEGER DEFAULT 0,
     max_point         INTEGER DEFAULT 0,
     games_played      INTEGER DEFAULT 0,
     mini_games_played INTEGER DEFAULT 0,
     difficulty_level INTEGER NOT NULL DEFAULT 3,
     stay_logged_in INTEGER DEFAULT 0,
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
    user_id INTEGER NOT NULL,
    quest_id INTEGER NOT NULL,
    progress INTEGER DEFAULT 0,
    is_completed INTEGER DEFAULT 0,
    reset_date TEXT,
    PRIMARY KEY (user_id, quest_id),
    FOREIGN KEY(user_id) REFERENCES users(id),
    FOREIGN KEY(quest_id) REFERENCES quests(id)
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



CREATE TABLE IF NOT EXISTS armor_definition (
    alias TEXT PRIMARY KEY,
    base_health INTEGER NOT NULL,
    metallic INTEGER NOT NULL DEFAULT 0,
    pass_damage INTEGER NOT NULL DEFAULT 0,
    layer_thresholds TEXT NOT NULL DEFAULT ''
);


CREATE TABLE IF NOT EXISTS zombie_template (
     alias TEXT PRIMARY KEY,
     hitpoints INTEGER NOT NULL,
     speed REAL NOT NULL,
     eat_dps REAL NOT NULL,
     wave_point_cost INTEGER NOT NULL DEFAULT 100,
     weight INTEGER NOT NULL DEFAULT 1000
);

CREATE TABLE IF NOT EXISTS zombie_behavior_template (
     id INTEGER PRIMARY KEY AUTOINCREMENT,

     zombie_alias TEXT NOT NULL
     REFERENCES zombie_template(alias) ON DELETE CASCADE,

     behavior_type TEXT NOT NULL,
     chain_order INTEGER NOT NULL DEFAULT 0,

    -- ArmorBehavior
      armor_alias TEXT
      REFERENCES armor_definition(alias),

    -- RangedAttackBehavior
       ranged_type TEXT,
       interval_ticks INTEGER,
       range INTEGER,
       extra_param INTEGER,

    -- SummonBehavior
       summon_type TEXT,
       summon_alias TEXT,
       summon_count INTEGER,
       hp_threshold INTEGER,

    -- DamageReactionBehavior
       reaction_type TEXT,
       param1 REAL DEFAULT 1.0,
       param2 REAL DEFAULT 1.0,

    -- MovementBehavior
       movement_type TEXT,
       movement_param REAL,

    -- WorldEffectBehavior
       world_effect_type TEXT,
       effect_interval INTEGER,
       effect_count INTEGER,

    -- AuraBehavior
       aura_type TEXT,
       aura_radius REAL,
       aura_interval INTEGER,

    -- DeathEffectBehavior
       death_effect_type TEXT,
       death_spawn_alias TEXT,
       death_spawn_count INTEGER,

    -- TransformBehavior
       transform_type TEXT,
       transform_interval INTEGER,
       transform_range INTEGER
    -- Sun steal
       sun_steal_max_amount INTEGER
);

CREATE INDEX IF NOT EXISTS idx_behavior_zombie
    ON zombie_behavior_template(zombie_alias, chain_order);
