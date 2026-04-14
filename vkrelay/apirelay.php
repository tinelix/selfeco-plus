<?php
	ob_start();
	error_reporting(E_ALL);

	header("Content-Type: text/json");
	if(strlen($_GET["goto"]) > 0)
		$request = $_GET["goto"];
	else
		$request = file_get_contents("php://input");
	
	$curl = curl_init($request);
	curl_exec($curl);

	$statusCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);

	if($statusCode === 301 && str_starts_with($request, "http://")) {
		$request = str_replace("http://", "https://", $request);
		$curl = curl_init($request);
		curl_exec($curl);
	}
	
	header("Content-length: " . ob_get_length());
	ob_flush();
