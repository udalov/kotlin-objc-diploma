@protocol Base
- (void)someBaseMethod;
@end


@protocol Derived<Base>
- (void)someDerivedMethod;
@end
