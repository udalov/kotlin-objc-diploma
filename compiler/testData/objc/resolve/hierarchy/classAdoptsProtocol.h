@interface NSObject
@end


@protocol P
- (void)protocolMethod;
@end


@interface A : NSObject<P>
@end
