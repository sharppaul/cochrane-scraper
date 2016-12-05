SELECT 
    SUBSTRING_INDEX(refs.cochrane_id,'.',1), 
    CASE WHEN LENGTH( SUBSTRING_INDEX(refs.cochrane_id,'.',-1)) > 4 THEN ''
    ELSE SUBSTRING_INDEX(refs.cochrane_id,'.',-1) END
    AS cochrane_pub, 
    REPLACE(refs.reference_pubmed_id, ',', '&#44') AS study_PMID,
    refs_author.year AS study_year,
    refs_author.title AS study_title,
    SUBSTRING_INDEX(refs_author.author_first,',',1) AS first_author_surname,
    LTRIM(SUBSTRING_INDEX(refs_author.author_first,',',-1)) AS first_author_firstname,
    refs_author.first_gender AS first_author_gender,
    refs_author.first_probability AS first_author_gender_probability,
    SUBSTRING_INDEX(refs_author.author_last,',',1) AS last_author_surname,
    LTRIM(SUBSTRING_INDEX(refs_author.author_last,',',-1)) AS last_author_firstname,
    refs_author.last_gender AS last_author_gender,
    refs_author.last_probability AS last_author_gender_probability
FROM 
    refs, refs_author
WHERE 
    refs.reference_pubmed_id = refs_author.pubmed_id
INTO OUTFILE '/tmp/cochrane_pubmed_references.csv'
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\r\n';
