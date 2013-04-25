@protocol Base
+ (void)baseMethod;
@end

@protocol Derived<Base>
+ (void)derivedMethod;
@end
