USE casdoor;

SET @shoprpa_logo = 'http://127.0.0.1:32742/shoprpa-static/shoprpa-wordmark.svg';
SET @shoprpa_icon = 'http://127.0.0.1:32742/shoprpa-static/shoprpa-icon.svg';

UPDATE application
SET
  name = 'shoprpa-app',
  organization = 'shoprpa-org',
  display_name = 'ShopRPA',
  logo = @shoprpa_logo,
  homepage_url = '',
  description = 'ShopRPA 로그인'
WHERE name IN ('shoprpa-app', 'example-app');

UPDATE application
SET
  display_name = 'ShopRPA Admin',
  logo = @shoprpa_logo,
  homepage_url = '',
  description = 'ShopRPA 관리자'
WHERE name = 'app-built-in';

UPDATE organization
SET
  name = 'shoprpa-org',
  default_application = 'shoprpa-app',
  display_name = 'ShopRPA',
  website_url = '',
  logo = @shoprpa_logo,
  logo_dark = @shoprpa_logo,
  favicon = @shoprpa_icon,
  default_avatar = @shoprpa_icon,
  country_codes = '["KR","US"]'
WHERE name IN ('shoprpa-org', 'example-org');

UPDATE organization
SET
  display_name = 'ShopRPA Admin',
  logo = @shoprpa_logo,
  logo_dark = @shoprpa_logo,
  favicon = @shoprpa_icon,
  default_avatar = @shoprpa_icon
WHERE name = 'built-in';

UPDATE user
SET
  password = '123456',
  password_type = 'plain',
  avatar = @shoprpa_icon
WHERE owner = 'built-in'
  AND name = 'admin';

CREATE TEMPORARY TABLE tmp_shoprpa_admin AS
SELECT *
FROM user
WHERE owner IN ('shoprpa-org', 'example-org')
  AND name IN ('admin', 'example-user')
ORDER BY name = 'admin' DESC
LIMIT 1;

UPDATE tmp_shoprpa_admin
SET
  owner = 'shoprpa-org',
  name = 'admin',
  id = '9a0f6b82-7fa5-4415-9d46-f7a0eac00001',
  password = '123456',
  password_type = 'plain',
  display_name = 'ShopRPA Admin',
  email = 'admin@shoprpa.local',
  phone = '01000000000',
  country_code = 'KR',
  avatar = @shoprpa_icon,
  is_admin = 1,
  is_forbidden = 0,
  is_deleted = 0,
  signup_application = 'shoprpa-app';

INSERT INTO user
SELECT *
FROM tmp_shoprpa_admin
ON DUPLICATE KEY UPDATE
  password = VALUES(password),
  password_type = VALUES(password_type),
  display_name = VALUES(display_name),
  email = VALUES(email),
  phone = VALUES(phone),
  country_code = VALUES(country_code),
  avatar = VALUES(avatar),
  is_admin = VALUES(is_admin),
  is_forbidden = VALUES(is_forbidden),
  is_deleted = VALUES(is_deleted),
  signup_application = VALUES(signup_application);

DROP TEMPORARY TABLE tmp_shoprpa_admin;

UPDATE role
SET
  owner = 'shoprpa-org',
  name = 'shoprpa-admin',
  display_name = 'ShopRPA Admin',
  users = '["shoprpa-org/admin"]'
WHERE owner IN ('shoprpa-org', 'example-org')
  AND name IN ('shoprpa-admin', 'example-role');

DELETE FROM user
WHERE owner IN ('shoprpa-org', 'example-org')
  AND name = 'example-user';
