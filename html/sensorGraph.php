<!DOCTYPE html>
<html>

<head>
    <meta charset="UTF-8">
    <title>Gas Usage</title>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
</head>

<body>
    <?php
    function getGasUsageData($dateFrom, $dateTo)
    {
        $conn = mysqli_connect("localhost", "pi", "1234");
        mysqli_select_db($conn, "gas_db");
        $query = "select date, sum(gas_usage) as total_gas_usage from gas_use where date >= '" . $dateFrom . "' and date <= '" . $dateTo . "' group by date";
        $result = mysqli_query($conn, $query);
        $data = array();
        while ($row = mysqli_fetch_array($result)) {
            $data[$row['date']] = $row['total_gas_usage'];
        }
        mysqli_close($conn);
        return $data;
    }

    $dateFrom = date('Y-m-d');
    $dateTo = date('Y-m-d');
    if (isset($_POST['submit'])) {
        $dateFrom = $_POST['dateFrom'];
        $dateTo = $_POST['dateTo'];
    }

    $data = getGasUsageData($dateFrom, $dateTo);
    ?>
    <div style="text-align: center;">
        <h1 align="center">Gas Usage</h1>
        <div class="spec">
            # <b>일자별 가스 사용량</b>
            <br></br>
			<form name="Filter" method="POST">
				<input type="date" name="dateFrom" value="<?php echo date('Y-m-d', strtotime('-6 day')); ?>" min="2021-08-07" />
      			<input type="date" name="dateTo" value="<?php echo date('Y-m-d'); ?>" />
                <input type="submit" name="submit" value="Search" />
            </form>
        </div>

        <div style="width: 50%; margin: 0 auto; text-align:center;">
            <canvas id="gasUsageChart"></canvas>
        </div>
    </div>
    <script>
        var data = <?php echo json_encode($data); ?>;

        var ctx = document.getElementById('gasUsageChart');
        var chart = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: Object.keys(data),
                datasets: [{
                    label: 'Total Day Gas Usage',
                    data: Object.values(data),
                    backgroundColor: 'rgba(54, 162, 235, 0.2)',
                    borderColor:'rgba(54, 162, 235, 1)',
                borderWidth: 1
            }]
        },
        options: {
            scales: {
                xAxes: [{
                    ticks: {
                        fontFamily: 'Arial, sans-serif',
                        autoSkip: false,
                        maxTicksLimit: 20
                    }
                }],
                yAxes: [{
                    ticks: {
                        fontFamily: 'Arial, sans-serif',
                        fontSize: 14,
                        beginAtZero: true
                    }
                }]
            }
        }
    });
</script>
