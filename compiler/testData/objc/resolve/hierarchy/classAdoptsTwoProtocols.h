@interface NSObject
@end


@protocol P
- (void)protocolMethod;
@end

@protocol Q
- (int)anotherMethod;
@end


@interface A : NSObject<P, Q>
@end
