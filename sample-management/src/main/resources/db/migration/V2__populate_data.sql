-- Address Store
INSERT INTO address_store (id, address_key, address) VALUES
    (1, 'OUR_ADDRESS', 'Hans-Meerwein Straße 2 35043 Marburg, Germany Phone: ++ 49 6421 28-65158 E-Mail: immunmonitoring.labor@uni-marburg.de') ON CONFLICT (id) DO NOTHING;

-- Report Authors
INSERT INTO report_authors (id, name, title)
VALUES
    (1, 'Prof. Dr. Stephan Becker', 'Director Institute of Virology'),
    (2, 'Dr. Verena Krähling', 'Laboratory Management') ON CONFLICT (id) DO NOTHING;