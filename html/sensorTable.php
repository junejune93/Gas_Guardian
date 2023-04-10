<!DOCTYPE html>
<html>
<head>
	<meta charset = "UTF-8">
	<meta http-equiv = "refresh" content = "60">
	<style type = "text/css">
		.spec{
			text-align:center;
		}
		.con{
			text-align:left;
		}
		table{
			font-size: 16px;
		} 
		</style>
</head>

<body>
	<h1 align = "center">Gas Usage</h1>
	<div class = "spec">
		# <b>기간별 가스 총 사용량</b>
		<br></br>
	<form name="Filter" method="POST">
    	<input type="date" name="dateFrom" value="<?php echo date('Y-m-d'); ?>" min="2021-08-07" />
    	<input type="date" name="dateFrom2" value="<?php echo date('Y-m-d'); ?>" />
    	<input type="submit" name="submit" value="Search"/>
</form>
	</div>

	<table border = '1' style = "width = 30%" align = "center">
	<tr align = "center">
		<th>id</th>
		<th>name</th>
		<th>date</th>
		<th>time</th>
		<th>gas_usage</th>
	</tr>
<?php
$new_date = date('Y-m-d', strtotime($_POST['dateFrom']));
$new_date1 = date('Y-m-d', strtotime($_POST['dateFrom2']));
if ($new_date == $new_date1) {
    $new_date = date('Y-m-d');
    $new_date1 = $new_date;
    $printDate = 0;
    $query = "select id, name, date, time, gas_usage from gas_use where date(date) = date('" . $new_date . "')";
} else {
    $printDate = 1;
    $query = "select id, name, date, time, gas_usage from gas_use where date(date) between date('" . $new_date . "') and date('" . $new_date1 . "')";
}

$conn = mysqli_connect("localhost", "pi", "1234");
mysqli_select_db($conn, "gas_db");
$result = mysqli_query($conn, $query);
$total_day_gas_usage = 0;
while ($row = mysqli_fetch_array($result)) {
    echo "<tr align = center>";
    echo '<th>' . $row['id'] . '</td>';
    echo '<th>' . $row['name'] . '</td>';
    echo '<th>' . $row['date'] . '</td>';
    echo '<th>' . $row['time'] . '</td>';
    echo '<th>' . $row['gas_usage'] . '</td>';
    echo "</tr>";
    $total_day_gas_usage += $row['gas_usage'];
}
mysqli_close($conn);
?>
<tr align="center">
    <td colspan="4"><b>Total Gas Usage:</b></td>
    <td style="font-weight:bold;"><?php echo $total_day_gas_usage; ?></td>
<tr align="center">
    <td colspan="4"><b>예상 가스비:</b></td>
    <td style="font-weight:bold;"><?php echo number_format($total_day_gas_usage * 0.1, 1); ?></td>
</tr>
</table>
</body>
</html>
