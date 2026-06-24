package kaleidostop.map.car_map.common.exception;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String resource, Long id) {
        super(resource + " с id " + id + " не найден(а)");
    }

    public NotFoundException(String resource, String identifier) {
        super(resource + " с идентификатором " + identifier + " не найден(а)");
    }
}
