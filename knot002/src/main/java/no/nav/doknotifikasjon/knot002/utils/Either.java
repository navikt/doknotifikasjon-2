package no.nav.doknotifikasjon.knot002.utils;

public class Either<Left, Right> {
    private final Left left;
    private final Right right;

    private Either(Left left, Right right) {
        this.left = left;
        this.right = right;
    }

    public static <Left, Right> Either<Left, Right> left(Left left){
        return new Either<Left, Right>(left, null);
    }

    public static <Left, Right> Either<Left, Right> right(Right right){
        return new Either<Left, Right>(null, right);
    }

    public Left getLeft() {
        return left;
    }

    public Right getRight() {
        return right;
    }
}
