package org.openelisglobal.config;

import jakarta.annotation.Nullable;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurerSupport;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig extends AsyncConfigurerSupport {

    // Pool BORNÉ (remplace SimpleAsyncTaskExecutor qui créait un thread illimité
    // par appel). Sans borne, si le store FHIR est lent/down, les threads de
    // transformation FHIR s'accumulaient sans limite -> épuisement threads/mémoire
    // -> crash du serveur entier. Valeurs modestes adaptées aux serveurs mono-site
    // (8 Go), surchargeables par propriétés.
    @Value("${org.openelisglobal.async.corePoolSize:2}")
    private int corePoolSize;

    @Value("${org.openelisglobal.async.maxPoolSize:8}")
    private int maxPoolSize;

    @Value("${org.openelisglobal.async.queueCapacity:100}")
    private int queueCapacity;

    @Value("${org.openelisglobal.async.keepAliveSeconds:60}")
    private int keepAliveSeconds;

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        // Laisse les threads du core expirer aussi quand le pool est inactif
        // (sobriété mémoire sur les petits serveurs).
        executor.setAllowCoreThreadTimeOut(true);
        executor.setThreadNamePrefix("oe-async-");
        // CallerRunsPolicy : si le pool ET la file sont saturés, la tâche s'exécute
        // dans le thread appelant. Cela RALENTIT le flux (contre-pression) au lieu
        // de perdre la tâche ou de crasher -> aucune perte silencieuse de données
        // médicales, et le serveur ne s'effondre pas sous une rafale.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // Laisse les tâches en cours se terminer proprement à l'arrêt.
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Override
    @Nullable
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncExceptionHandler();
    }
}
