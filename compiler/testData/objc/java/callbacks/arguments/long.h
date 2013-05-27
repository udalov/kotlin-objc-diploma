#import <Foundation/NSObject.h>

typedef void (*fun_t)(long long);

@interface A : NSObject
+ (void) invokeWith123456789123456789: (fun_t) fun;
@end
