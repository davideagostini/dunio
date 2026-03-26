-keep class com.davideagostini.summ.data.entity.** { *; }
-keep class com.davideagostini.summ.data.session.**Document { *; }
-keep class com.davideagostini.summ.data.dao.**Document { *; }
-keep class com.davideagostini.summ.widget.data.**Document { *; }

# Glance pulls WorkManager transitively. In release builds, Room must still be
# able to reflectively locate WorkDatabase_Impl during startup initialization.
-keep class androidx.work.impl.WorkDatabase { *; }
-keep class androidx.work.impl.WorkDatabase$* { *; }
-keep class androidx.work.impl.WorkDatabase_Impl { *; }
-keep class androidx.work.impl.WorkDatabase_Impl$* { *; }
-keep class androidx.work.impl.model.** { *; }
