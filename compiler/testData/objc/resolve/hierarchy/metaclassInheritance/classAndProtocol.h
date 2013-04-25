@protocol BaseProtocol
+ (void)baseProtocolMethod;
@end

@interface BaseClass
+ (void)baseClassMethod;
@end


@interface Derived : BaseClass<BaseProtocol>
+ (void)derivedMethod;
@end
