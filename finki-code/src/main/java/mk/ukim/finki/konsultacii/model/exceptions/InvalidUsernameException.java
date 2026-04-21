package mk.ukim.finki.konsultacii.model.exceptions;

public class InvalidUsernameException extends RuntimeException {
    public InvalidUsernameException(String username){
        System.out.printf("User with username %s does not exist", username);
    }

    public InvalidUsernameException(){
        System.out.printf("User does not exist");
    }
}
