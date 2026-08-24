package cn.academy.ability.context;

import cn.lambdalib2.util.Debug;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.util.function.Function;

public class ClientContext extends Context {

    @SuppressWarnings("rawtypes")
    static final Multimap<Class<? extends Context>, Function<Context, ClientContext>>
            clientTypes = HashMultimap.create();

    public final Context parent;

    public ClientContext(Context _parent) {
        super(_parent.player, _parent.skill);
        parent = _parent;
    }

    @Override
    public Context.Status getStatus() {
        return parent.status;
    }

    @Override
    public void terminate() {
        parent.terminate();
    }

    @SuppressWarnings("unchecked")
    public static void scanAndRegister() {
        Type annoType = Type.getType(RegClientContext.class);
        for (ModFileScanData scan : ModList.get().getAllScanData()) {
            for (ModFileScanData.AnnotationData a : scan.getAnnotations()) {
                if (!annoType.equals(a.annotationType())) continue;
                String className = a.clazz().getClassName();
                try {
                    Class<?> clazz = Class.forName(className, false, ClientContext.class.getClassLoader());
                    if (!ClientContext.class.isAssignableFrom(clazz)) {
                        Debug.warn("@RegClientContext on non-ClientContext class: " + className);
                        continue;
                    }
                    RegClientContext anno = clazz.getAnnotation(RegClientContext.class);
                    Class<? extends Context> parentType = anno.value();

                    Constructor<?> ctor = clazz.getConstructor(parentType);
                    clientTypes.put(parentType, parent -> {
                        try {
                            return (ClientContext) ctor.newInstance(parent);
                        } catch (Exception ex) {
                            throw new RuntimeException("Failed to construct ClientContext: " + className, ex);
                        }
                    });
                } catch (Throwable ex) {
                    Debug.error("Failed to register ClientContext " + className, ex);
                }
            }
        }
    }
}
