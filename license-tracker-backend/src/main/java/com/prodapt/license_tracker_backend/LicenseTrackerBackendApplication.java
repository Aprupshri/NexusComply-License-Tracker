package com.prodapt.license_tracker_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableScheduling
@EnableMethodSecurity(prePostEnabled = true)
public class LicenseTrackerBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(LicenseTrackerBackendApplication.class, args);
	}

}
