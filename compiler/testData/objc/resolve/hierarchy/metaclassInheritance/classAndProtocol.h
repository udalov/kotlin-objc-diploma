@protocol BaseProtocol
+ (void)baseProtocolMethod;
@end

@interface BaseClass
+ (void)baseClassMethod;
@end


@interface Derived : BaseClass<BaseProtocol>
+ (void)derivedClassMethod;
@end

@protocol DerivedProtocol <BaseProtocol>
+ (void)derivedProtocolMethod;
@end
