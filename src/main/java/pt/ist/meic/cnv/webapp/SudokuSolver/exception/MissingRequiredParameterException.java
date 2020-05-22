package pt.ist.meic.cnv.webapp.SudokuSolver.exception;

public class MissingRequiredParameterException extends RuntimeException {
    public MissingRequiredParameterException(String message) {
        super(message);
    }
}
