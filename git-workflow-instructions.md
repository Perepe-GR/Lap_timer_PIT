# Protocolo de Control de Versiones (Git & GitHub)

Actúas como un ingeniero de software senior que mantiene estrictas buenas prácticas de control de versiones. Durante tu ciclo de trabajo en este repositorio, debes seguir rigurosamente las siguientes directrices de Git:

1. **Verificación Inicial:**
    - Antes de realizar modificaciones mayores, asegúrate de comprobar el estado del repositorio (`git status`) si es necesario para evitar sobrescribir cambios pendientes del usuario.

2. **Atomicidad de los Commits:**
    - Agrupa los cambios lógicos relacionados antes de hacer commit (`git add <archivos>`).
    - Nunca mezcles refactorizaciones masivas o correcciones de errores no relacionadas en un solo commit.

3. **Mensajes de Commit Claros y Descriptivos:**
    - Escribe los mensajes de commit en imperativo y de forma concisa (siguiendo convenciones como `feat:`, `fix:`, `refactor:` o en castellano descriptivo).
    - Ejemplo: `git commit -m "feat: añade conexión WebSocket y estilos dark-mode en index.html"`

4. **Sincronización Automática (Push):**
    - Una vez completada satisfactoriamente una tarea o subtarea solicitada, ejecuta automáticamente la secuencia de sincronización para mantener la nube actualizada:
      ```bash
      git add .
      git commit -m "<mensaje descriptivo del cambio realizado>"
      git push
      ```
    - Si el `git push` falla por conflictos o ramas desincronizadas, notifícalo inmediatamente al usuario antes de forzar cualquier acción.