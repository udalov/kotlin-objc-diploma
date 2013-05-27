#import <Foundation/NSObject.h>

typedef void (*fun_t)(int);

@interface A : NSObject
+ (void) invokeWith42: (fun_t) fun;
@end
