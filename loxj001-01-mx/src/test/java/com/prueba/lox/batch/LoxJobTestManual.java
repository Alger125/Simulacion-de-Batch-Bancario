package com.prueba.lox.batch;

import org.junit.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class LoxJobTestManual {

    @Test
    public void testManualLaunch() {
        // 1. Cargamos el contexto.
        // TIP: Si lox-job.xml ya tiene un <import> hacia lox-beans.xml, solo necesitas cargar uno.
        // Si no, dejamos ambos pero asegúrate de que lox-beans aparezca PRIMERO.
        String[] configFiles = {
                "META-INF/spring/batch/beans/lox-beans.xml",
                "META-INF/spring/batch/jobs/lox-job.xml"
        };

        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(configFiles);

        try {
            // 2. Obtenemos el lanzador y el job
            JobLauncher jobLauncher = (JobLauncher) context.getBean("jobLauncher");
            Job job = (Job) context.getBean("LOXJ162-01-MX");

            // 3. Preparamos los parámetros - ¡CAMBIO CLAVE AQUÍ!
            // Cambiamos "fecha" por "odate" para que coincida con #{jobParameters['odate']} del XML
            JobParameters params = new JobParametersBuilder()
                    .addString("odate", "20260212")
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            // 4. Ejecutamos
            System.out.println(">>> Iniciando Job manualmente...");
            JobExecution execution = jobLauncher.run(job, params);

            System.out.println(">>> ESTADO FINAL: " + execution.getExitStatus());

        } catch (Exception e) {
            System.err.println(">>> ERROR DURANTE LA EJECUCIÓN:");
            e.printStackTrace();
        } finally {
            context.close();
        }
    }
}