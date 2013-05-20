@interface NSObject
@end

@interface Base : NSObject
+ (void)baseMethod;
@end

@interface Derived : Base
+ (void)derivedMethod;
@end
