package space.dawdawich.configuration.provider

import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Component

/**
 * The `ApplicationContextProvider` class is responsible for providing access to the application context
 * throughout the application. It implements the `ApplicationContextAware` interface and is annotated with
 * @Component for automatic detection and registration as a component bean.
 *
 * @property context The application context instance.
 */
@Component
class ApplicationContextProvider : ApplicationContextAware {

    companion object {
        private lateinit var context: ApplicationContext

        fun getApplicationContext(): ApplicationContext = context
    }

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        context = applicationContext
    }
}
