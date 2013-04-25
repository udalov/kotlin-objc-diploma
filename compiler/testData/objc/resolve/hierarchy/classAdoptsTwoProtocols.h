@protocol P
- (void)protocolMethod;
@end

@protocol Q
- (int)anotherMethod;
@end


@interface A<P, Q>
@end
