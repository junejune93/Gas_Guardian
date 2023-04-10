<form name="Filter" method="POST">
    <select name="sensor" >
      <option value="illu, temp, humi">=== 선택 ===</option>
      <option value="gas usage" selected>gas usage</option>
    </select>
    <input type="date" name="dateFrom" value="<?php echo date('Y-m-d'); ?>" min="2021-08-07" />
    <input type="date" name="dateFrom2" value="<?php echo date('Y-m-d'); ?>" />
    <input type="submit" name="submit" value="Search"/>
</form>
<?php
	$sensor_name = $_POST['sensor'];
	$new_date = date('Y-m-d', strtotime($_POST['dateFrom']));
	$new_date1 = date('Y-m-d', strtotime($_POST['dateFrom2']));
	
        if($sensor_name == '')
                $sensor_name = 'gas_usage';

	if($new_date == $new_date1) {
		$new_date = date('Y-m-d');
		$new_date1 = $new_date;
		$printDate = 0;
		$query = "select name, time, $sensor_name from sensor where date(date) = date('".$new_date."')";
	}
	else 
	{
		$printDate = 1;
		$query = "select name,date, time, $sensor_name from sensor where date(date) between date('".$new_date."') and date('".$new_date1."')";
	} 
	$conn = mysqli_connect("localhost", "pi", "1234");
	mysqli_set_charset($conn, "UTF-8");
	mysqli_select_db($conn, "gas_db");
	$result = mysqli_query($conn, $query);

	$data = array(array('KSH_SQL','gas_usage'));

	if($result)
	{
		while($row = mysqli_fetch_array($result))
		{
			if($printDate == 0)
				array_push($data, array($row['time'], intval($row['illu']),intval($row['temp']) ,intval($row['humi'])));
			else	
				array_push($data, array($row['date']." ".$row['time'], intval($row['gas_usage'])));
		}
	}

	$options = array(
			'title' => 'illumination temperature humidity',
			'width' => 1000, 'height' => 400,
			'curveType' => 'function'
			);

?>

<script src="//www.google.com/jsapi"></script>
<script>
var data = <?=json_encode($data) ?>;
var options = <?= json_encode($options) ?>;

google.load('visualization', '1.0', {'packages':['corechart']});

google.setOnLoadCallback(function() {
	var chart = new google.visualization.LineChart(document.querySelector('#chart_div'));
	chart.draw(google.visualization.arrayToDataTable(data), options);
	});
	</script>
<div id="chart_div"></div>
