package com.Restaurant.service;

import java.io.IOException;
import java.util.List;

public interface ReportService {

	String exportSalesCsv(java.time.LocalDate from, java.time.LocalDate to, Long restaurantId) throws IOException;
	String exportSalesTxt(java.time.LocalDate from, java.time.LocalDate to, Long restaurantId) throws IOException;
	String exportMenuCsv(Long restaurantId) throws IOException;
	String exportMenuTxt(Long restaurantId) throws IOException;
	String exportUsersCsv() throws IOException;
	String exportUsersTxt() throws IOException;
	List<String> listAvailableReports();
}
