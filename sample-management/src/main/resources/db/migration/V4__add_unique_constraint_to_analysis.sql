ALTER TABLE analysis
ADD CONSTRAINT uc_analysis_sample_type UNIQUE (sample_id, analysis_type_id);