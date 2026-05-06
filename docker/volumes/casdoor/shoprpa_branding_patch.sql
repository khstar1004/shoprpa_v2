USE casdoor;

SET @shoprpa_logo = 'http://127.0.0.1:32742/shoprpa-static/shoprpa-wordmark.svg';
SET @shoprpa_icon = 'http://127.0.0.1:32742/shoprpa-static/shoprpa-icon.svg';

UPDATE application
SET
  display_name = 'Shoprpa',
  logo = @shoprpa_logo,
  homepage_url = '',
  description = 'Shoprpa 로그인'
WHERE name = 'example-app';

UPDATE application
SET
  display_name = 'Shoprpa Admin',
  logo = @shoprpa_logo,
  homepage_url = '',
  description = 'Shoprpa 관리자'
WHERE name = 'app-built-in';

UPDATE organization
SET
  display_name = 'Shoprpa',
  website_url = '',
  logo = @shoprpa_logo,
  logo_dark = @shoprpa_logo,
  favicon = @shoprpa_icon,
  default_avatar = @shoprpa_icon,
  country_codes = '["KR","US"]'
WHERE name = 'example-org';

UPDATE organization
SET
  display_name = 'Shoprpa Admin',
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
WHERE owner = 'example-org'
  AND name = 'example-user'
LIMIT 1;

UPDATE tmp_shoprpa_admin
SET
  name = 'admin',
  id = '9a0f6b82-7fa5-4415-9d46-f7a0eac00001',
  password = '123456',
  password_type = 'plain',
  display_name = 'Shoprpa Admin',
  email = 'admin@shoprpa.local',
  phone = '01000000000',
  country_code = 'KR',
  avatar = @shoprpa_icon,
  is_admin = 1,
  is_forbidden = 0,
  is_deleted = 0,
  signup_application = 'example-app';

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
SET users = '["example-org/example-user","example-org/admin"]'
WHERE owner = 'example-org'
  AND name = 'example-role';
