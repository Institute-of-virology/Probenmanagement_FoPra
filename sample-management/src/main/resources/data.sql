-- Studies
INSERT INTO study (id, study_name, start_date, end_date, sponsor, sender1, sender2, sender3, expected_number_of_subjects, expected_number_of_sample_deliveries, remark)
VALUES
    (1, 'Sample Study 1', CURRENT_DATE, CURRENT_DATE, 'Virologie', 'John', 'Robert', 'Peter', '100', '50', 'From Marburg'),
    (2, 'Sample Study 2', CURRENT_DATE, CURRENT_DATE, 'Medizin', 'Alex', 'Johnny', 'Chris', '10', '5', 'From Giessen'),
    (3, 'Sample Study 3', CURRENT_DATE, CURRENT_DATE, 'Informatik', 'Maria', 'Natasha', 'Adam', '15', '20', 'From Ginseldorf') on conflict do nothing;

-- Analysis Types
INSERT INTO analysis_type (id, analysis_name, analysis_description)
VALUES
    (1, 'Analysis 1', 'This Analysis tests for something'),
    (2, 'Analysis 2', 'This Analysis tests for something else'),
    (3, 'Analysis 3', 'This Analysis tests for something else again'),
    (4, 'Analysis 4', 'Yet another Analysis') on conflict do nothing;

-- Study-AnalysisType relationships (join table)
-- INSERT INTO study_analysis_types (study_id, analysis_type_id) VALUES
--                                                                   (1, 1), (1, 2),
--                                                                   (2, 3), (2, 4),
--                                                                   (3, 1), (3, 3), (3, 4);

-- Subjects
INSERT INTO subject (id, alias, study_id) VALUES
                                              (1, 1, 1),
                                              (2, 2, 2),
                                              (3, 1, 3),
                                              (4, 2, 3),
                                              (5, 3, 3) on conflict do nothing;

-- Sample Deliveries
INSERT INTO sample_delivery (id, delivery_date, study_id) VALUES
                                                              (1, CURRENT_DATE, 1),
                                                              (2, CURRENT_DATE, 2),
                                                              (3, CURRENT_DATE, 3) on conflict do nothing;

-- Samples
-- Assuming you already inserted studies with IDs 1, 2, 3
-- and subjects with IDs 1, 2, 3, 4, 5
-- and sample_deliveries with IDs 1, 2, 3

INSERT INTO sample (
    id,
    coordinates,
    sample_date,
    sample_amount,
    sample_barcode,
    sample_type,
    visits,
    subject_id,
    sample_delivery_id,
    study_id,
    validated
) VALUES
      (1, '0,0', CURRENT_DATE, '10ml', 'ABC123', 'plasma', 1, 1, 1, 1, true),
      (2, '1,1', CURRENT_DATE, '20ml', 'XYZ456', 'serum', 2, 1, 1, 1, true),
      (3, '5,2', CURRENT_DATE, '100ml', 'ABCDEF123', 'plasma', 7, 2, 2, 2, false),
      (4, '1,1', CURRENT_DATE, '20ml', 'XYZ45678', 'serum', 2, 3, 3, 3, false),
      (5, '5,0', CURRENT_DATE, '80ml', 'ABCDEF12345', 'blood', 5, 3, 3, 3, false),
      (6, '1,1', CURRENT_DATE, '200ml', 'XYZ458', 'plasma', 2, 4, 3, 3, true) on conflict do nothing;


-- Analyses
INSERT INTO analysis (id, sample_id, analysis_type_id, analysis_result, analysis_date) VALUES
                                                                                           (1, 1, 1, '7.0', CURRENT_DATE),
                                                                                           (2, 1, 2, '100.0', CURRENT_DATE),
                                                                                           (3, 2, 1, '13.0', CURRENT_DATE),
                                                                                           (4, 3, 3, '5.0', CURRENT_DATE),
                                                                                           (5, 3, 4, '3.0', CURRENT_DATE),
                                                                                           (6, 4, 1, '7.0', CURRENT_DATE),
                                                                                           (7, 4, 3, '5.0', CURRENT_DATE),
                                                                                           (8, 4, 4, '3.0', CURRENT_DATE),
                                                                                           (9, 5, 1, '', CURRENT_DATE),
                                                                                           (10, 5, 3, '', CURRENT_DATE),
                                                                                           (11, 5, 4, '', CURRENT_DATE),
                                                                                           (12, 6, 1, '7.0', CURRENT_DATE),
                                                                                           (13, 6, 3, '5.0', CURRENT_DATE),
                                                                                           (14, 6, 4, '3.0', CURRENT_DATE) on conflict do nothing;

-- Address Store
-- Insert only if not exists (PostgreSQL example)
INSERT INTO address_store (id, address_key, address) VALUES
    (1, 'OUR_ADDRESS', 'Hans-Meerwein Straße 2 35043 Marburg, Germany Phone: ++ 49 6421 28-65158 E-Mail: immunmonitoring.labor@uni-marburg.de') on conflict do nothing;

-- Report Authors
INSERT INTO report_authors (id, name, title)
VALUES
    (1, 'Prof. Dr. Stephan Becker', 'Director Institute of Virology'),
    (2, 'Dr. Verena Krähling', 'Laboratory Management') on conflict do nothing;
