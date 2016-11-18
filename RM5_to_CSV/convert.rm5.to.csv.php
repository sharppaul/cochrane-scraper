#!/usr/bin/php
<?
## Wim Otte (w.m.otte@umcutrecht.nl)

error_reporting( E_STRICT );

// check input arguments.
if( sizeof( $argv ) < 3 )
{
    print( "*** Usage: $argv[0]  <input>.rm5  <output>.csv\n\n" );
    exit( 1 );
}

// get input and output files.
$infile = $argv[ 1 ];
$outfile = $argv[ 2 ];


// check if input rm5-file exists.
if( ! file_exists( $infile ) )
{
    print( "*** ERROR ***: $infile does not exist!\n" );
    exit( 1 );
}

// FUNCTIONS ###############################################################################

/**
 * Process Data output.
 */
function processOutcomeObject( $obj, $type, $outfile, $overall_title, $inputFile )
{
    $compType = $type;

    $name = $obj->NAME;
    $name = str_replace( ',', '_' , $name );
    $name = trim( preg_replace('/\s\s+/', ' ', $name ) );

    $id = $obj[ 0 ][ 'ID' ]; // e.g. "CMP-001.02"
    preg_match( '/(?P<start>\w+)-(?P<comparisonID>\d+).(?P<subsetID>\d+)/', $id, $matches );
    $compID = $matches[ 'comparisonID' ];
    $subID = $matches[ 'subsetID' ];

    $group_label_1 = $obj->GROUP_LABEL_1;
    $group_label_2 = $obj->GROUP_LABEL_2;
    $graph_label_1 = $obj->GRAPH_LABEL_1;
    $graph_label_2 = $obj->GRAPH_LABEL_2;
    
    $group_label_1 = str_replace( ',', '_' , $group_label_1 );
    $group_label_2 = str_replace( ',', '_' , $group_label_2 );
    $graph_label_1 = str_replace( ',', '_' , $graph_label_1 );
    $graph_label_2 = str_replace( ',', '_' , $graph_label_2 );
    
    $group_label_1 = trim( preg_replace('/\s\s+/', ' ', $group_label_1 ) );
    $group_label_2 = trim( preg_replace('/\s\s+/', ' ', $group_label_2 ) );
    $graph_label_1 = trim( preg_replace('/\s\s+/', ' ', $graph_label_1 ) );
    $graph_label_2 = trim( preg_replace('/\s\s+/', ' ', $graph_label_2 ) );

    $subgroups = $obj[ 0 ][ 'SUBGROUPS' ];
    $subgroup_test = $obj[ 0 ][ 'SUBGROUP_TEST' ];
    $chi2 = $obj[ 0 ][ 'CHI2' ];
    $ci_end = $obj[ 0 ][ 'CI_END' ];
    $ci_start = $obj[ 0 ][ 'CI_START' ];
    $ci_study = $obj[ 0 ][ 'CI_STUDY' ];
    $ci_total = $obj[ 0 ][ 'CI_TOTAL' ];
    $df = $obj[ 0 ][ 'DF' ];
    $effect_measure = $obj[ 0 ][ 'EFFECT_MEASURE' ];
    $effect_size = $obj[ 0 ][ 'EFFECT_SIZE' ];
    $estimable = $obj[ 0 ][ 'ESTIMABLE' ];
    $events_1 = $obj[ 0 ][ 'EVENTS_1' ];
    $events_2 = $obj[ 0 ][ 'EVENTS_2' ];
    $i2 = $obj[ 0 ][ 'I2' ];
    $i2_q = $obj[ 0 ][ 'I2_Q' ];
    $log_ci_end = $obj[ 0 ][ 'LOG_CI_END' ];
    $log_ci_start = $obj[ 0 ][ 'LOG_CI_START' ];
    $log_effect_size = $obj[ 0 ][ 'LOG_EFFECT_SIZE' ];
    $method = $obj[ 0 ][ 'METHOD' ];
    $no = $obj[ 0 ][ 'NO' ];
    $o_e = $obj[ 0 ][ 'O_E' ];
    $p_chi2 = $obj[ 0 ][ 'P_CHI2' ];
    $p_q = $obj[ 0 ][ 'P_Q' ];
    $p_z = $obj[ 0 ][ 'P_Z' ];
    $q = $obj[ 0 ][ 'Q' ];
    $random = $obj[ 0 ][ 'RANDOM' ];
    $scale = $obj[ 0 ][ 'SCALE' ];
    $sort_by = $obj[ 0 ][ 'SORT_BY' ];
    $studies = $obj[ 0 ][ 'STUDIES' ];
    $swap_events = $obj[ 0 ][ 'SWAP_EVENTS' ];
    $tau2 = $obj[ 0 ][ 'TAU2' ];
    $total_1 = $obj[ 0 ][ 'TOTAL_1' ];
    $total_2 = $obj[ 0 ][ 'TOTAL_2' ];
    $totals = $obj[ 0 ][ 'TOTALS' ];
    $var = $obj[ 0 ][ 'VAR' ];
    $weight = $obj[ 0 ][ 'WEIGHT' ];
    $z = $obj[ 0 ][ 'Z' ];

    // get url from input rm5 file (i.e. last digit is CD)
    //i.e. 4.rm5 -> "http://onlinelibrary.wiley.com/enhanced/doi/10.1002/14651858.CD000004.pub2";
    $withoutExt = preg_replace( '/\\.[^.\\s]{3,4}$/', '', $inputFile );
    preg_match( '/(\d+)\D*\z/', $withoutExt, $mat );
    $cd = str_pad( $mat[ 0 ], 6, '0', STR_PAD_LEFT );
    $cochraneURL = "http://onlinelibrary.wiley.com/enhanced/doi/10.1002/14651858.CD" . $cd . ".pub2";

    $out = "$inputFile,$cochraneURL,$overall_title,$compType,$id,$compID,$subID,\"$name\",\"$group_label_1\",\"$group_label_2\",\"$graph_label_1\",\"$graph_label_2\"," .
        "$chi2,$ci_end,$ci_start,$ci_study,$ci_total,$df,$effect_measure,$effect_size,$estimable,$events_1,$events_2," .
        "$i2,$i2_q,$log_ci_end,$log_ci_start,$log_effect_size,$method,$no,$o_e,$p_chi2,$p_q,$p_z,$q," .
        "$random,$scale,$sort_by,$studies,$subgroups,$subgroup_test,$swap_events,$tau2,$total_1,$total_2,$totals,$var,$weight,$z";

    $out_sub = ",,,,,,,,,,,,,,,,,,,,,,,"; // empty sub-group output columns

    // get individual studies
    $dich_datas = $obj->DICH_DATA;
    $cont_datas = $obj->CONT_DATA;

    foreach( $dich_datas as $dich_data )
    {
        $out_data = processDataObject( $dich_data, "DICH" );
        $data = $out . "," . $out_data . "," . $out_sub . "\n";
        file_put_contents( $outfile, $data, FILE_APPEND | LOCK_EX );
    }

    foreach( $cont_datas as $cont_data )
    {
        $out_data = processDataObject( $cont_data, "CONT" );
        $data = $out . "," . $out_data . "," . $out_sub . "\n";
        file_put_contents( $outfile, $data, FILE_APPEND | LOCK_EX );
    }

    // process SUBGROUPS
    if( $subgroups == "YES" )
    {
        $cont_subgroups = $obj->CONT_SUBGROUP; 
        $dich_subgroups = $obj->DICH_SUBGROUP;

        // CONT subgroups
        foreach( $cont_subgroups as $cont_subgroup )
        {
            $out_sub = processSubGroup( $cont_subgroup, "CONT" );

            // get individual studies
            $cont_datas = $cont_subgroup->CONT_DATA;

            foreach( $cont_datas as $cont_data )
            {
                $out_data = processDataObject( $cont_data, "CONT" );
                $data = $out . "," . $out_data . "," . $out_sub . "\n";
                file_put_contents( $outfile, $data, FILE_APPEND | LOCK_EX );
            }
        }

        // DICH subgroups
        foreach( $dich_subgroups as $dich_subgroup )
        {
            $out_sub = processSubGroup( $dich_subgroup, "DICH" );

            // get individual studies
            $dich_datas = $dich_subgroup->DICH_DATA;

            foreach( $dich_datas as $dich_data )
            {
                $out_data = processDataObject( $dich_data, "DICH" );
                $data = $out . "," . $out_data . "," . $out_sub . "\n";
                file_put_contents( $outfile, $data, FILE_APPEND | LOCK_EX );
            }
        }

    } // end subgroups 
}

/**
 * Process data object.
 */
function processDataObject( $obj, $type )
{
    $study_id = $obj[ 0 ][ 'STUDY_ID' ]; 

    $study_id = str_replace( ',', '_' , $study_id );
    $study_id = trim( preg_replace('/\s\s+/', ' ', $study_id ) );

    // parse study year
    preg_match( '/(18[0-9][0-9])|(19[0-9][0-9])|(20[0-1][0-9])/', $study_id, $matches );
    $study_year = $matches[ 0 ];
    if( $study_year < 1800 ) { $study_year = ""; }

    $study_ci_end = $obj[ 0 ][ 'CI_END' ]; 
    $study_ci_start = $obj[ 0 ][ 'CI_START' ]; 
    $study_effect_size = $obj[ 0 ][ 'EFFECT_SIZE' ]; 
    $study_estimable = $obj[ 0 ][ 'ESTIMABLE' ]; 
    $study_events_1 = $obj[ 0 ][ 'EVENTS_1' ]; 
    $study_events_2 = $obj[ 0 ][ 'EVENTS_2' ]; 
    $study_log_ci_end = $obj[ 0 ][ 'LOG_CI_END' ]; 
    $study_log_ci_start = $obj[ 0 ][ 'LOG_CI_START' ]; 
    $study_log_effect_size = $obj[ 0 ][ 'LOG_EFFECT_SIZE' ]; 

    $study_o_e = $obj[ 0 ][ 'O_E' ]; 
    $study_order = $obj[ 0 ][ 'ORDER' ]; 
    $study_se = $obj[ 0 ][ 'SE' ]; 
    $study_total_1 = $obj[ 0 ][ 'TOTAL_1' ]; 
    $study_total_2 = $obj[ 0 ][ 'TOTAL_2' ]; 
    $study_var = $obj[ 0 ][ 'VAR' ]; 
    $study_weight = $obj[ 0 ][ 'WEIGHT' ];
    $study_mean_1 = $obj[ 0 ][ 'MEAN_1' ]; 
    $study_mean_2 = $obj[ 0 ][ 'MEAN_2' ]; 
    $study_sd_1 = $obj[ 0 ][ 'SD_1' ]; 
    $study_sd_2 = $obj[ 0 ][ 'SD_2' ]; 

    $out = "$study_id,$study_year,$study_ci_end,$study_ci_start,$study_effect_size,$study_estimable,$study_events_1,$study_events_2," .
        "$study_log_ci_end,$study_log_ci_start,$study_log_effect_size," .
        "$study_o_e,$study_order,$study_se,$study_total_1,$study_total_2," .
        "$study_var,$study_weight,$study_mean_1,$study_mean_2,$study_sd_1,$study_sd_2";

    return( $out );
} 

/**
 * Process subgroup.
 */
function processSubGroup( $cont_subgroup, $type )
{
    $subgroup_name = $cont_subgroup->NAME;
    $subgroup_name = str_replace( ',', '_' , $subgroup_name );
    $subgroup_name = trim( preg_replace('/\s\s+/', ' ', $subgroup_name ) ); 
    
    $subgroup_chi2 = $cont_subgroup[ 0 ][ 'CHI2' ];
    $subgroup_ci_end = $cont_subgroup[ 0 ][ 'CI_END' ];
    $subgroup_ci_start = $cont_subgroup[ 0 ][ 'CI_START' ];
    $subgroup_df = $cont_subgroup[ 0 ][ 'DF' ];
    $subgroup_effect_size = $cont_subgroup[ 0 ][ 'EFFECT_SIZE' ];
    $subgroup_estimable = $cont_subgroup[ 0 ][ 'ESTIMABLE' ];
    $subgroup_events_1 = $cont_subgroup[ 0 ][ 'EVENTS_1' ];
    $subgroup_events_2 = $cont_subgroup[ 0 ][ 'EVENTS_2' ];
    $subgroup_i2 = $cont_subgroup[ 0 ][ 'I2' ];
    $subgroup_id = $cont_subgroup[ 0 ][ 'ID' ];
    $subgroup_log_ci_end = $cont_subgroup[ 0 ][ 'LOG_CI_END' ];
    $subgroup_log_ci_start = $cont_subgroup[ 0 ][ 'LOG_CI_START' ];
    $subgroup_log_effect_size = $cont_subgroup[ 0 ][ 'LOG_EFFECT_SIZE' ];
    $subgroup_no = $cont_subgroup[ 0 ][ 'NO' ];
    $subgroup_p_chi2 = $cont_subgroup[ 0 ][ 'P_CHI2' ];
    $subgroup_p_z = $cont_subgroup[ 0 ][ 'P_Z' ];
    $subgroup_studies = $cont_subgroup[ 0 ][ 'STUDIES' ];
    $subgroup_tau2 = $cont_subgroup[ 0 ][ 'TAU2' ];
    $subgroup_total_1 = $cont_subgroup[ 0 ][ 'TOTAL_1' ];
    $subgroup_total_2 = $cont_subgroup[ 0 ][ 'TOTAL_2' ];
    $subgroup_weight = $cont_subgroup[ 0 ][ 'WEIGHT' ];
    $subgroup_z = $cont_subgroup[ 0 ][ 'Z' ];

    $out = "$type,$subgroup_name,$subgroup_chi2,$subgroup_ci_end,$subgroup_ci_start,$subgroup_df,$subgroup_effect_size," .
        "$subgroup_estimable,$subgroup_events_1,$subgroup_events_2,$subgroup_i2,$subgroup_id,$subgroup_log_ci_end," .
        "$subgroup_log_ci_start,$subgroup_log_effect_size,$subgroup_no,$subgroup_p_chi2,$subgroup_p_z," .
        "$subgroup_studies,$subgroup_tau2,$subgroup_total_1,$subgroup_total_2,$subgroup_weight,$subgroup_z";

    return( $out );
}

/**
 * Get column names.
 */
function getColnames()
{
    $colnames_out = "rm5_input_file,cochrane_url,title,type,id,compID,subID,name,group_label_1,group_label_2,graph_label_1,graph_label_2," .
        "chi2,ci_end,ci_start,ci_study,ci_total,df,effect_measure,effect_size,estimable,events_1,events_2," .
        "i2,i2_q,log_ci_end,log_ci_start,log_effect_size,method,no,o_e,p_chi2,p_q,p_z,q," .
        "random,scale,sort_by,studies,subgroups,subgroup_test,swap_events,tau2,total_1,total_2,totals,var,weight,z";

    $colnames_data = "study_id,study_year,study_ci_end,study_ci_start,study_effect_size,study_estimable,study_events_1," .
        "study_events_2,study_log_ci_end,study_log_ci_start,study_log_effect_size," .
        "study_o_e,study_order,study_se,study_total_1,study_total_2," .
        "study_var,study_weight,study_mean_1,study_mean_2,study_sd_1,study_sd_2";

    $colnames_subgroup = "subgroup_type,subgroup_name,subgroup_chi2,subgroup_ci_end,subgroup_ci_start,subgroup_df,subgroup_effect_size," .
        "subgroup_estimable,subgroup_events_1,subgroup_events_2,subgroup_i2,subgroup_id,subgroup_log_ci_end," .
        "subgroup_log_ci_start,subgroup_log_effect_size,subgroup_no,subgroup_p_chi2,subgroup_p_z," .
        "subgroup_studies,subgroup_tau2,subgroup_total_1,subgroup_total_2,subgroup_weight,subgroup_z";

    $colnames = "$colnames_out,$colnames_data,$colnames_subgroup\n";
    return( $colnames );
}

// END FUNCTIONS ###########################################################################


// read rm5 xml input file
$xml = simplexml_load_file( $infile ) or die( "*** Error: Cannot read: $infile\n" );

// overall study title
//$overall_title = $xml->MAIN_TEXT->SUMMARY->TITLE;
$overall_title = $xml->COVER_SHEET->TITLE;
$overall_title = str_replace( ',', '_' , $overall_title );
$overall_title = trim( preg_replace('/\s\s+/', ' ', $overall_title ) );
$colnames = getColnames();
$colsWritten = false;

// loop through subsets
foreach( $xml->children() as $child )
{ 
    // grab child with the data
    if( $child[ 0 ][ 'CALCULATED_DATA' ] == 'YES' )
    {
        $comparisons = $child->children();

        foreach( $comparisons as $comparison )
        {
            $name = $comparison->NAME;
            $name = str_replace( ',', '_' , $name );
            $name = trim( preg_replace('/\s\s+/', ' ', $name ) );

            $cont_outcomes = $comparison->CONT_OUTCOME;
            $dich_outcomes = $comparison->DICH_OUTCOME;

            // only write colnames if data is present in rm5-file.
            if( ( sizeof( $cont_outcomes ) > 0 ) || ( sizeof( $dich_outcomes ) > 0 ) && ( $colsWritten == false ) )
            {
                // write columns to output file:
                file_put_contents( $outfile, $colnames, LOCK_EX );
                $colsWritten = true;
            }

            foreach( $cont_outcomes as $cont_outcome )
            {
                processOutcomeObject( $cont_outcome, "CONT", $outfile, $overall_title, $infile );
            }

            foreach( $dich_outcomes as $dich_outcome )
            {
                processOutcomeObject( $dich_outcome, "DICH", $outfile, $overall_title, $infile );
            }
        } // comparisons loop
    } // child with 'CALCULATED_DATA'
} // root xml children


?>
