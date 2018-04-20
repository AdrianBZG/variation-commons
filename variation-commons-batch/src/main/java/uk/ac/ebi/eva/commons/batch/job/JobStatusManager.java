/*
 * Copyright 2014-2018 EMBL - European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.ebi.eva.commons.batch.job;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.util.StringUtils;


import uk.ac.ebi.eva.commons.batch.exception.NoJobToExecuteException;
import uk.ac.ebi.eva.commons.batch.exception.NoParametersHaveBeenPassedException;
import uk.ac.ebi.eva.commons.batch.exception.NoPreviousJobExecutionException;

import java.util.Date;
import java.util.Objects;

/**
 * Utility class to change job / step status
 */
public class JobStatusManager {

    public static void checkIfPropertiesHaveBeenProvided(JobParameters jobParameters)
            throws NoParametersHaveBeenPassedException {
        if (jobParameters == null || jobParameters.isEmpty() || allJobParametersValuesAreNull(jobParameters)) {
            throw new NoParametersHaveBeenPassedException();
        }
    }

    private static boolean allJobParametersValuesAreNull(JobParameters jobParameters) {
        return jobParameters.getParameters().values().stream().map(JobParameter::getValue).allMatch(Objects::isNull);
    }

    public static void checkIfJobNameHasBeenDefined(String jobName) throws NoJobToExecuteException {
        if (!StringUtils.hasText(jobName)) {
            throw new NoJobToExecuteException();
        }
    }

    public static void markLastJobAsFailed(JobRepository jobRepository, String jobName,
                                           JobParameters jobParameters) throws NoPreviousJobExecutionException {
        JobExecution lastJobExecution = jobRepository.getLastJobExecution(jobName, jobParameters);
        if (lastJobExecution == null) {
            throw new NoPreviousJobExecutionException(jobName, jobParameters);
        }

        Date currentTime = new Date();
        lastJobExecution.setEndTime(currentTime);
        lastJobExecution.setStatus(BatchStatus.FAILED);
        lastJobExecution.setExitStatus(
                lastJobExecution.getExitStatus().replaceExitCode("FAILED").addExitDescription("Manually failed job")
        );
        jobRepository.update(lastJobExecution);

        for (StepExecution stepExecution : lastJobExecution.getStepExecutions()) {
            stepExecution.setEndTime(currentTime);
            stepExecution.setStatus(BatchStatus.FAILED);
            stepExecution.setExitStatus(lastJobExecution.getExitStatus().replaceExitCode("FAILED"));
            jobRepository.update(stepExecution);
        }
    }

}
